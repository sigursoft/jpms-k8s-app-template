package com.sigursoft.jpms.k8s.app;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationTests {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String JSON = "application/json";
    private static final String SENSOR_JSON = "{\"id\":\"s-001\",\"description\":\"Temperature sensor\"}";

    private HttpServer server;
    private ExecutorService executor;

    @BeforeEach
    public void startServer() throws IOException {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server = Application.start(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), executor);
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
        executor.close();
    }

    private HttpResponse<String> send(String method, String path, String contentType, String body)
            throws IOException, InterruptedException {
        var uri = URI.create("http://localhost:" + server.getAddress().getPort() + path);
        var builder = HttpRequest.newBuilder(uri).method(method,
                body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        return CLIENT.send(builder.build(), BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send("GET", path, null, null);
    }

    private HttpResponse<String> post(String body) throws IOException, InterruptedException {
        return send("POST", "/sensors", JSON, body);
    }

    private static void assertJson(int status, String body, HttpResponse<String> response) {
        assertEquals(status, response.statusCode());
        assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElse(null));
        assertEquals(body, response.body());
    }

    @Test
    public void testRootReturnsJsonGreeting() throws Exception {
        assertJson(200, "{\"message\":\"Hello World!\"}", get("/"));
    }

    @Test
    public void testRootHeadHasNoBody() throws Exception {
        var response = send("HEAD", "/", null, null);
        assertEquals(200, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    public void testOptionsListsAllowedMethods() throws Exception {
        var response = send("OPTIONS", "/sensors", null, null);
        assertEquals(200, response.statusCode());
        assertEquals("GET, POST, OPTIONS", response.headers().firstValue("Allow").orElse(null));
    }

    @Test
    public void testUnsupportedMethodIsRejected() throws Exception {
        var response = send("DELETE", "/sensors/s-001", null, null);
        assertJson(405, "{\"error\":\"Method not allowed: DELETE\"}", response);
        assertEquals("GET, OPTIONS", response.headers().firstValue("Allow").orElse(null));
    }

    @Test
    public void testUnknownPathReturnsJsonNotFound() throws Exception {
        assertJson(404, "{\"error\":\"Resource not found: /nope\"}", get("/nope"));
    }

    @Test
    public void testSensorsListIsInitiallyEmpty() throws Exception {
        assertJson(200, "[]", get("/sensors"));
    }

    @Test
    public void testCreateAndReadSensor() throws Exception {
        var created = post(SENSOR_JSON);
        assertJson(201, SENSOR_JSON, created);
        assertEquals("/sensors/s-001", created.headers().firstValue("Location").orElse(null));

        assertJson(200, SENSOR_JSON, get("/sensors/s-001"));
        assertJson(200, "[" + SENSOR_JSON + "]", get("/sensors"));
    }

    @Test
    public void testCreateAcceptsContentTypeParameters() throws Exception {
        assertEquals(201, send("POST", "/sensors", "Application/JSON; charset=UTF-8", SENSOR_JSON).statusCode());
    }

    @Test
    public void testCreateDuplicateSensorConflicts() throws Exception {
        post(SENSOR_JSON);
        assertJson(409, "{\"error\":\"Sensor already exists: s-001\"}", post(SENSOR_JSON));
    }

    @Test
    public void testCreateWithInvalidJsonIsBadRequest() throws Exception {
        var response = post("{\"id\":");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().startsWith("{\"error\":\"Malformed JSON"), response.body());
        assertJson(400, "{\"error\":\"Property 'description' is required\"}", post("{\"id\":\"a\"}"));
    }

    @Test
    public void testCreateRequiresJsonContentType() throws Exception {
        assertJson(415, "{\"error\":\"Content-Type must be application/json\"}",
                send("POST", "/sensors", "text/plain", SENSOR_JSON));
        assertEquals(415, send("POST", "/sensors", null, SENSOR_JSON).statusCode());
    }

    @Test
    public void testCreateRejectsOversizedBody() throws Exception {
        String body = "{\"id\":\"a\",\"description\":\"" + "x".repeat(64 * 1024) + "\"}";
        assertJson(413, "{\"error\":\"Request body exceeds 65536 bytes\"}", post(body));
    }

    @Test
    public void testMissingSensorIsNotFound() throws Exception {
        assertJson(404, "{\"error\":\"Sensor not found: nope\"}", get("/sensors/nope"));
    }

    @Test
    public void testEmptySensorIdIsNotFound() throws Exception {
        assertJson(404, "{\"error\":\"Resource not found: /sensors/\"}", get("/sensors/"));
    }

    @Test
    public void testCreateRejectsTooLongDescription() throws Exception {
        String body = "{\"id\":\"a\",\"description\":\"" + "x".repeat(1025) + "\"}";
        assertJson(400, "{\"error\":\"Property 'description' must be at most 1024 characters\"}", post(body));
    }

    @Test
    public void testCreateRejectsDuplicateKeys() throws Exception {
        var response = post("{\"id\":\"a\",\"id\":\"b\",\"description\":\"d\"}");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().startsWith("{\"error\":\"Malformed JSON"), response.body());
    }

    @Test
    public void testOversizedBodyDoesNotBreakFollowingRequests() throws Exception {
        post("{\"id\":\"a\",\"description\":\"" + "x".repeat(200 * 1024) + "\"}");
        assertJson(200, "[]", get("/sensors"));
    }

    @Test
    public void testStartFailureIsLoggedWithReason() throws Exception {
        var logger = (Logger) org.slf4j.LoggerFactory.getLogger(Application.class.getName());
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try (var occupied = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var otherExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), occupied.getLocalPort());
            assertNull(Application.tryStart(address, otherExecutor));
        } finally {
            logger.detachAppender(appender);
        }
        String message = appender.list.getLast().getFormattedMessage();
        assertTrue(message.startsWith("Failed to create HTTP server: "), message);
        assertFalse(message.contains("%s") || message.contains("{0}"), message);
        assertTrue(message.length() > "Failed to create HTTP server: ".length(), message);
    }
}
