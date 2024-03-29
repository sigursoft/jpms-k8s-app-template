package com.sigursoft.jpms.k8s.app;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.System.Logger.Level;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Application {

    private static final System.Logger LOGGER = System.getLogger(Application.class.getName());

    public static void main(String[] args) {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
        LOGGER.log(Level.INFO, "Starting server");
        forEach(exchange -> {
            String path = exchange.getRequestURI().toString();
            if (ROOT_CONTEXT_PATH.equals(path)) {
                switch (exchange.getRequestMethod()) {
                    case GET -> respond(exchange, OK, ROOT_RESOURCE);
                    case HEAD -> respond(exchange, OK, EMPTY_RESPONSE_BODY);
                    case OPTIONS -> {
                        exchange.getResponseHeaders().add(ALLOW, ALLOWED_METHODS);
                        respond(exchange, OK, EMPTY_RESPONSE_BODY);
                    }
                    default -> {
                        exchange.getResponseHeaders().add(ALLOW, ALLOWED_METHODS);
                        respond(exchange, METHOD_NOT_ALLOWED, EMPTY_RESPONSE_BODY);
                    }
                }
            } else {
                respond(exchange, NOT_FOUND, EMPTY_RESPONSE_BODY);
            }
        });
        LOGGER.log(Level.INFO, "Server started");
    }

    private static void forEach(HttpHandler handler) {
        HttpServer httpServer;
        try {
            httpServer = HttpServer.create(ADDRESS, BACKLOG);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create HTTP server: %s", e.getMessage());
            return;
        }
        var context = httpServer.createContext(ROOT_CONTEXT_PATH);

        context.setHandler(handler);

        httpServer.setExecutor(threadPoolExecutor);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> httpServer.stop(NOW)));
    }

    private static void respond(HttpExchange exchange, final int httpStatus, final byte[] message) {
        // Closing an exchange without consuming all request body is not an error but may make the underlying
        // TCP connection unusable for following exchanges (think HTTP 1.1 pipelining).
        consumeInputStream(exchange.getRequestBody());
        exchange.getResponseHeaders().add(CONTENT_TYPE, TEXT_PLAIN);
        try (exchange) {
            if (message.length > 0) {
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
    private static final byte[] ROOT_RESOURCE = ("Hello World! \n").getBytes(UTF_8);
    private static final byte[] EMPTY_RESPONSE_BODY = new byte[0];
    private static final InetSocketAddress ADDRESS = new InetSocketAddress("0.0.0.0", 9000);
    private static final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    private static final int BACKLOG = 100;
    private static final String ROOT_CONTEXT_PATH = "/";
    private static final String GET = "GET";
    private static final String HEAD = "HEAD";
    private static final String OPTIONS = "OPTIONS";
    private static final String ALLOW = "Allow";
    private static final String ALLOWED_METHODS = "GET, OPTIONS";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_PLAIN = "text/plain; charset=utf-8";
    private static final int OK = 200;
    private static final int NOT_FOUND = 404;
    private static final int METHOD_NOT_ALLOWED = 405;
    private static final int NOW = 0;
}
