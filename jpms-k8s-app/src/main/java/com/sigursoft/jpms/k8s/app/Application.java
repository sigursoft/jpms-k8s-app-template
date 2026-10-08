package com.sigursoft.jpms.k8s.app;

import com.sigursoft.jpms.k8s.model.Sensor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.System.Logger.Level;

public class Application {

    private static final System.Logger LOGGER = System.getLogger(Application.class.getName());

    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Starting server");
        HttpServer httpServer;
        try {
            httpServer = start(ADDRESS);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create HTTP server: %s", e.getMessage());
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> httpServer.stop(NOW)));
        LOGGER.log(Level.INFO, "Server started");
    }

    /**
     * Starts a server with an empty sensor repository bound to the given address.
     */
    static HttpServer start(InetSocketAddress address) throws IOException {
        var repository = new SensorRepository();
        HttpServer httpServer = HttpServer.create(address, BACKLOG);
        httpServer.createContext(ROOT_CONTEXT_PATH).setHandler(exchange -> {
            try {
                handle(exchange, repository);
            } catch (RuntimeException e) {
                LOGGER.log(Level.ERROR, "Failed to handle request", e);
                respond(exchange, INTERNAL_SERVER_ERROR, JsonCodec.writeError("Internal server error"));
            }
        });
        httpServer.setExecutor(threadPoolExecutor);
        httpServer.start();
        return httpServer;
    }

    private static void handle(HttpExchange exchange, SensorRepository repository) {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (ROOT_CONTEXT_PATH.equals(path)) {
            switch (method) {
                case GET -> respond(exchange, OK, ROOT_RESOURCE);
                case HEAD -> respond(exchange, OK, EMPTY_RESPONSE_BODY);
                default -> respondToOtherMethod(exchange, method, ROOT_ALLOWED_METHODS);
            }
        } else if (SENSORS_PATH.equals(path)) {
            switch (method) {
                case GET -> respond(exchange, OK, JsonCodec.writeSensors(repository.findAll()));
                case POST -> createSensor(exchange, repository);
                default -> respondToOtherMethod(exchange, method, SENSORS_ALLOWED_METHODS);
            }
        } else if (path.startsWith(SENSOR_PATH_PREFIX)) {
            String id = path.substring(SENSOR_PATH_PREFIX.length());
            switch (method) {
                case GET -> repository.find(id).ifPresentOrElse(
                        sensor -> respond(exchange, OK, JsonCodec.writeSensor(sensor)),
                        () -> respond(exchange, NOT_FOUND, JsonCodec.writeError("Sensor not found: " + id)));
                default -> respondToOtherMethod(exchange, method, SENSOR_ALLOWED_METHODS);
            }
        } else {
            respond(exchange, NOT_FOUND, JsonCodec.writeError("Resource not found: " + path));
        }
    }

    private static void createSensor(HttpExchange exchange, SensorRepository repository) {
        if (!isJson(exchange.getRequestHeaders().getFirst(CONTENT_TYPE))) {
            respond(exchange, UNSUPPORTED_MEDIA_TYPE, JsonCodec.writeError("Content-Type must be " + APPLICATION_JSON));
            return;
        }
        byte[] body;
        try {
            body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BODY_BYTES + 1);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read request body: %s", e.getMessage());
            respond(exchange, BAD_REQUEST, JsonCodec.writeError("Failed to read request body"));
            return;
        }
        if (body.length > MAX_REQUEST_BODY_BYTES) {
            respond(exchange, CONTENT_TOO_LARGE,
                    JsonCodec.writeError("Request body exceeds " + MAX_REQUEST_BODY_BYTES + " bytes"));
            return;
        }
        Sensor sensor;
        try {
            sensor = JsonCodec.readSensor(body);
        } catch (IllegalArgumentException e) {
            respond(exchange, BAD_REQUEST, JsonCodec.writeError(e.getMessage()));
            return;
        }
        if (!repository.add(sensor)) {
            respond(exchange, CONFLICT, JsonCodec.writeError("Sensor already exists: " + sensor.id()));
            return;
        }
        exchange.getResponseHeaders().add(LOCATION, SENSOR_PATH_PREFIX + sensor.id());
        respond(exchange, CREATED, JsonCodec.writeSensor(sensor));
    }

    private static void respondToOtherMethod(HttpExchange exchange, String method, String allowedMethods) {
        exchange.getResponseHeaders().add(ALLOW, allowedMethods);
        if (OPTIONS.equals(method)) {
            respond(exchange, OK, EMPTY_RESPONSE_BODY);
        } else {
            respond(exchange, METHOD_NOT_ALLOWED, JsonCodec.writeError("Method not allowed: " + method));
        }
    }

    private static boolean isJson(String contentType) {
        return contentType != null
                && contentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT).equals(APPLICATION_JSON);
    }

    private static void respond(HttpExchange exchange, final int httpStatus, final byte[] message) {
        // Closing an exchange without consuming all request body is not an error but may make the underlying
        // TCP connection unusable for following exchanges (think HTTP 1.1 pipelining).
        consumeInputStream(exchange.getRequestBody());
        exchange.getResponseHeaders().add(CONTENT_TYPE, APPLICATION_JSON_UTF8);
        try (exchange) {
            if (message.length > 0 && !HEAD.equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(httpStatus, message.length);
                exchange.getResponseBody().write(message);
            } else {
                exchange.sendResponseHeaders(httpStatus, -1);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write response: %s", e.getMessage());
        }
    }

    private static void consumeInputStream(final InputStream is) {
        if (is == null)
            return;
        try {
            while (true) {
                /* null loop */
                if (is.read() == -1) break;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read request body: %s", e.getMessage());
        }
    }
    private static final byte[] ROOT_RESOURCE = JsonCodec.writeMessage("Hello World!");
    private static final byte[] EMPTY_RESPONSE_BODY = new byte[0];
    private static final InetSocketAddress ADDRESS = new InetSocketAddress("0.0.0.0", 9000);
    private static final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    private static final int BACKLOG = 100;
    private static final int MAX_REQUEST_BODY_BYTES = 64 * 1024;
    private static final String ROOT_CONTEXT_PATH = "/";
    private static final String SENSORS_PATH = "/sensors";
    private static final String SENSOR_PATH_PREFIX = SENSORS_PATH + "/";
    private static final String GET = "GET";
    private static final String HEAD = "HEAD";
    private static final String POST = "POST";
    private static final String OPTIONS = "OPTIONS";
    private static final String ALLOW = "Allow";
    private static final String ROOT_ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String SENSORS_ALLOWED_METHODS = "GET, POST, OPTIONS";
    private static final String SENSOR_ALLOWED_METHODS = "GET, OPTIONS";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String LOCATION = "Location";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";
    private static final int OK = 200;
    private static final int CREATED = 201;
    private static final int BAD_REQUEST = 400;
    private static final int NOT_FOUND = 404;
    private static final int METHOD_NOT_ALLOWED = 405;
    private static final int CONFLICT = 409;
    private static final int CONTENT_TOO_LARGE = 413;
    private static final int UNSUPPORTED_MEDIA_TYPE = 415;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int NOW = 0;
}
