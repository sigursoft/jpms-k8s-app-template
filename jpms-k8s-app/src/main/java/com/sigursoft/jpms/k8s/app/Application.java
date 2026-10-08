package com.sigursoft.jpms.k8s.app;

import com.sigursoft.jpms.k8s.model.Sensor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        LOGGER.info("Starting server");
        configureServerLimits();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        HttpServer httpServer = tryStart(listenAddress(), executor);
        if (httpServer == null) {
            executor.close();
            // A non-zero status lets Kubernetes see the failure and restart the pod.
            System.exit(1);
            return;
        }
        // Let in-flight requests finish: stop accepting exchanges, then wait for the running handlers.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            httpServer.stop(SHUTDOWN_GRACE_SECONDS);
            executor.close();
        }));
        LOGGER.info("Server started on port {}", httpServer.getAddress().getPort());
    }

    /**
     * Bounds how long a client may take to send a request or read a response. The JDK server waits forever by
     * default, which lets slow clients hold connections open. Values given on the command line take precedence.
     */
    private static void configureServerLimits() {
        System.getProperties().putIfAbsent("sun.net.httpserver.maxReqTime", MAX_REQUEST_SECONDS);
        System.getProperties().putIfAbsent("sun.net.httpserver.maxRspTime", MAX_RESPONSE_SECONDS);
    }

    private static InetSocketAddress listenAddress() {
        String port = System.getenv(PORT_ENV);
        if (port == null || port.isBlank()) {
            return new InetSocketAddress(ANY_ADDRESS, DEFAULT_PORT);
        }
        try {
            return new InetSocketAddress(ANY_ADDRESS, Integer.parseInt(port.strip()));
        } catch (IllegalArgumentException e) {
            // Covers both a malformed number and a port outside 0..65535
            LOGGER.warn("Ignoring invalid {} value '{}', using {}", PORT_ENV, port, DEFAULT_PORT);
            return new InetSocketAddress(ANY_ADDRESS, DEFAULT_PORT);
        }
    }

    /**
     * Starts the server, logging the reason and returning {@code null} if it cannot be started.
     */
    static HttpServer tryStart(InetSocketAddress address, ExecutorService executor) {
        try {
            return start(address, executor);
        } catch (IOException e) {
            LOGGER.error("Failed to create HTTP server: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Starts a server with an empty sensor repository that serves requests on the given executor.
     * The caller owns the executor and is responsible for closing it.
     */
    static HttpServer start(InetSocketAddress address, ExecutorService executor) throws IOException {
        var repository = new SensorRepository();
        HttpServer httpServer = HttpServer.create(address, BACKLOG);
        httpServer.createContext(ROOT_CONTEXT_PATH).setHandler(exchange -> {
            try {
                handle(exchange, repository);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to handle request", e);
                respond(exchange, HttpStatus.INTERNAL_SERVER_ERROR, JsonCodec.writeError("Internal server error"));
            }
        });
        httpServer.setExecutor(executor);
        httpServer.start();
        return httpServer;
    }

    private enum HttpStatus {
        OK(200),
        CREATED(201),
        BAD_REQUEST(400),
        NOT_FOUND(404),
        METHOD_NOT_ALLOWED(405),
        CONFLICT(409),
        CONTENT_TOO_LARGE(413),
        UNSUPPORTED_MEDIA_TYPE(415),
        INTERNAL_SERVER_ERROR(500),
        INSUFFICIENT_STORAGE(507);

        private final int code;

        HttpStatus(int code) {
            this.code = code;
        }
    }

    private enum Method {
        GET, HEAD, POST, OPTIONS,
        /** Any method this server does not implement. */
        OTHER;

        static Method of(String name) {
            for (Method method : values()) {
                if (method.name().equals(name)) {
                    return method;
                }
            }
            return OTHER;
        }
    }

    private sealed interface Route {
        record Root() implements Route {
        }

        record Sensors() implements Route {
        }

        record SensorById(String id) implements Route {
        }

        record Unknown(String path) implements Route {
        }
    }

    private static Route route(String path) {
        if (ROOT_CONTEXT_PATH.equals(path)) {
            return new Route.Root();
        } else if (SENSORS_PATH.equals(path)) {
            return new Route.Sensors();
        } else if (path.startsWith(SENSOR_PATH_PREFIX) && path.length() > SENSOR_PATH_PREFIX.length()) {
            return new Route.SensorById(path.substring(SENSOR_PATH_PREFIX.length()));
        }
        return new Route.Unknown(path);
    }

    private static void handle(HttpExchange exchange, SensorRepository repository) {
        Method method = Method.of(exchange.getRequestMethod());
        switch (route(exchange.getRequestURI().getPath())) {
            case Route.Root _ -> {
                switch (method) {
                    case GET -> respond(exchange, HttpStatus.OK, ROOT_RESOURCE);
                    case HEAD -> respond(exchange, HttpStatus.OK, EMPTY_RESPONSE_BODY);
                    default -> respondToOtherMethod(exchange, method, ROOT_ALLOWED_METHODS);
                }
            }
            case Route.Sensors _ -> {
                switch (method) {
                    case GET -> respond(exchange, HttpStatus.OK, JsonCodec.writeSensors(repository.findAll()));
                    case POST -> createSensor(exchange, repository);
                    default -> respondToOtherMethod(exchange, method, SENSORS_ALLOWED_METHODS);
                }
            }
            case Route.SensorById(var id) -> {
                switch (method) {
                    case GET -> repository.find(id).ifPresentOrElse(
                            sensor -> respond(exchange, HttpStatus.OK, JsonCodec.writeSensor(sensor)),
                            () -> respond(exchange, HttpStatus.NOT_FOUND,
                                    JsonCodec.writeError("Sensor not found: " + id)));
                    default -> respondToOtherMethod(exchange, method, SENSOR_ALLOWED_METHODS);
                }
            }
            case Route.Unknown(var path) ->
                    respond(exchange, HttpStatus.NOT_FOUND, JsonCodec.writeError("Resource not found: " + path));
        }
    }

    private sealed interface CreateResult {
        record Created(Sensor sensor) implements CreateResult {
        }

        record Rejected(HttpStatus status, String message) implements CreateResult {
        }
    }

    private static void createSensor(HttpExchange exchange, SensorRepository repository) {
        CreateResult result = parseSensor(exchange);
        if (result instanceof CreateResult.Created(var sensor)) {
            result = switch (repository.add(sensor)) {
                case ADDED -> result;
                case DUPLICATE -> new CreateResult.Rejected(HttpStatus.CONFLICT,
                        "Sensor already exists: " + sensor.id());
                case FULL -> new CreateResult.Rejected(HttpStatus.INSUFFICIENT_STORAGE,
                        "Sensor limit reached: " + repository.maxSize());
            };
        }
        switch (result) {
            case CreateResult.Created(var sensor) -> {
                exchange.getResponseHeaders().add(LOCATION, SENSOR_PATH_PREFIX + sensor.id());
                respond(exchange, HttpStatus.CREATED, JsonCodec.writeSensor(sensor));
            }
            case CreateResult.Rejected(var status, var message) ->
                    respond(exchange, status, JsonCodec.writeError(message));
        }
    }

    /**
     * Reads and validates the sensor in the request body without touching the repository or the response.
     */
    private static CreateResult parseSensor(HttpExchange exchange) {
        if (!isJson(exchange.getRequestHeaders().getFirst(CONTENT_TYPE))) {
            return new CreateResult.Rejected(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Content-Type must be " + APPLICATION_JSON);
        }
        byte[] body;
        try {
            body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BODY_BYTES + 1);
        } catch (IOException e) {
            LOGGER.warn("Failed to read request body: {}", e.getMessage());
            return new CreateResult.Rejected(HttpStatus.BAD_REQUEST, "Failed to read request body");
        }
        if (body.length > MAX_REQUEST_BODY_BYTES) {
            return new CreateResult.Rejected(HttpStatus.CONTENT_TOO_LARGE,
                    "Request body exceeds " + MAX_REQUEST_BODY_BYTES + " bytes");
        }
        try {
            return new CreateResult.Created(JsonCodec.readSensor(body));
        } catch (IllegalArgumentException e) {
            return new CreateResult.Rejected(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static void respondToOtherMethod(HttpExchange exchange, Method method, String allowedMethods) {
        exchange.getResponseHeaders().add(ALLOW, allowedMethods);
        if (method == Method.OPTIONS) {
            respond(exchange, HttpStatus.OK, EMPTY_RESPONSE_BODY);
        } else {
            respond(exchange, HttpStatus.METHOD_NOT_ALLOWED,
                    JsonCodec.writeError("Method not allowed: " + exchange.getRequestMethod()));
        }
    }

    private static boolean isJson(String contentType) {
        return contentType != null
                && contentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT).equals(APPLICATION_JSON);
    }

    private static void respond(HttpExchange exchange, HttpStatus status, final byte[] message) {
        exchange.getResponseHeaders().add(CONTENT_TYPE, APPLICATION_JSON_UTF8);
        try (exchange) {
            if (message.length > 0 && Method.of(exchange.getRequestMethod()) != Method.HEAD) {
                exchange.sendResponseHeaders(status.code, message.length);
                exchange.getResponseBody().write(message);
            } else {
                exchange.sendResponseHeaders(status.code, -1);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to write response: {}", e.getMessage());
        }
    }


    private static final byte[] ROOT_RESOURCE = JsonCodec.writeMessage("Hello World!");
    private static final byte[] EMPTY_RESPONSE_BODY = new byte[0];
    private static final String ANY_ADDRESS = "0.0.0.0";
    private static final String PORT_ENV = "PORT";
    private static final int DEFAULT_PORT = 9000;
    private static final String MAX_REQUEST_SECONDS = "30";
    private static final String MAX_RESPONSE_SECONDS = "30";
    private static final int BACKLOG = 100;
    private static final int MAX_REQUEST_BODY_BYTES = 64 * 1024;
    private static final String ROOT_CONTEXT_PATH = "/";
    private static final String SENSORS_PATH = "/sensors";
    private static final String SENSOR_PATH_PREFIX = SENSORS_PATH + "/";
    private static final String ALLOW = "Allow";
    private static final String ROOT_ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String SENSORS_ALLOWED_METHODS = "GET, POST, OPTIONS";
    private static final String SENSOR_ALLOWED_METHODS = "GET, OPTIONS";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String LOCATION = "Location";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";
    private static final int SHUTDOWN_GRACE_SECONDS = 5;
}
