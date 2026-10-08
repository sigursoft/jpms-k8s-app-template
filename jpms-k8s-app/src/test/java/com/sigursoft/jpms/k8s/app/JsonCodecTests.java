package com.sigursoft.jpms.k8s.app;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import com.sigursoft.jpms.k8s.model.Sensor;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JsonCodecTests {

    private static final String EXPECTED_OBJECT = "Request body must be a single JSON object";

    private static Sensor read(String json) {
        return JsonCodec.readSensor(json.getBytes(UTF_8));
    }

    private static String readError(String json) {
        return assertThrows(IllegalArgumentException.class, () -> read(json)).getMessage();
    }

    @Test
    public void testReadSensor() {
        assertEquals(new Sensor("s-001", "Temperature sensor"),
                read("{\"id\":\"s-001\",\"description\":\"Temperature sensor\"}"));
    }

    @Test
    public void testReadSensorIgnoresUnknownPropertiesAndComments() {
        String json = """
                {
                  // a comment
                  "unit": {"name": "celsius", "values": [1, 2]},
                  "id": "s-001", /* another comment */
                  "description": "Temperature sensor"
                }
                """;
        assertEquals(new Sensor("s-001", "Temperature sensor"), read(json));
    }

    @Test
    public void testReadSensorRejectsMalformedJson() {
        assertTrue(readError("{\"id\": ").startsWith("Malformed JSON"));
    }

    @Test
    public void testReadSensorRejectsNonObject() {
        assertEquals(EXPECTED_OBJECT, readError("[]"));
    }

    @Test
    public void testReadSensorRejectsEmptyBody() {
        assertEquals(EXPECTED_OBJECT, readError(""));
    }

    @Test
    public void testReadSensorRejectsTrailingContent() {
        assertEquals(EXPECTED_OBJECT,
                readError("{\"id\":\"a\",\"description\":\"b\"} {}"));
    }

    @Test
    public void testReadSensorRejectsMissingId() {
        assertTrue(readError("{\"description\":\"b\"}").contains("'id'"));
    }

    @Test
    public void testReadSensorRejectsInvalidId() {
        assertTrue(readError("{\"id\":\"a/b\",\"description\":\"b\"}").contains("'id'"));
        assertTrue(readError("{\"id\":\"\",\"description\":\"b\"}").contains("'id'"));
    }

    @Test
    public void testReadSensorRejectsNonStringProperty() {
        assertEquals("Property 'id' must be a string", readError("{\"id\":1,\"description\":\"b\"}"));
        assertEquals("Property 'id' must be a string", readError("{\"id\":1.5,\"description\":\"b\"}"));
        assertEquals("Property 'description' must be a string", readError("{\"id\":\"a\",\"description\":true}"));
    }

    @Test
    public void testReadSensorRejectsNestedValue() {
        assertEquals("Property 'description' must be a string",
                readError("{\"id\":\"a\",\"description\":{\"text\":\"b\"}}"));
    }

    @Test
    public void testReadSensorRejectsMissingDescription() {
        assertEquals("Property 'description' is required", readError("{\"id\":\"a\",\"description\":null}"));
    }

    @Test
    public void testReadSensorRejectsDuplicateKeys() {
        assertTrue(readError("{\"id\":\"a\",\"id\":\"b\",\"description\":\"d\"}").startsWith("Malformed JSON"));
    }

    @Test
    public void testWriteSensor() {
        assertEquals("{\"id\":\"s-001\",\"description\":\"Say \\\"hi\\\"\"}",
                new String(JsonCodec.writeSensor(new Sensor("s-001", "Say \"hi\"")), UTF_8));
    }

    @Test
    public void testWriteSensors() {
        assertEquals("[{\"id\":\"a\",\"description\":\"x\"},{\"id\":\"b\",\"description\":\"y\"}]",
                new String(JsonCodec.writeSensors(List.of(new Sensor("a", "x"), new Sensor("b", "y"))), UTF_8));
        assertEquals("[]", new String(JsonCodec.writeSensors(List.of()), UTF_8));
    }

    @Test
    public void testWriteMessageAndError() {
        assertEquals("{\"message\":\"hello\"}", new String(JsonCodec.writeMessage("hello"), UTF_8));
        assertEquals("{\"error\":\"oops\"}", new String(JsonCodec.writeError("oops"), UTF_8));
    }

    @Test
    public void testRoundTrip() {
        Sensor sensor = new Sensor("s.1_x-2", "Zażółć gęślą jaźń ☃");
        assertEquals(sensor, JsonCodec.readSensor(JsonCodec.writeSensor(sensor)));
    }
}
