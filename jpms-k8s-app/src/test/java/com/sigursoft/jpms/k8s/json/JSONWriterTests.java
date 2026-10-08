package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JSONWriterTests {

    @Test
    public void testSimpleObject() {
        StringWriter sw = new StringWriter();
        new JSONWriter(sw)
            .object()
            .key("name")
            .value("Alice")
            .endObject();
        assertEquals("{\"name\":\"Alice\"}", sw.toString());
    }

    @Test
    public void testSimpleArray() {
        StringWriter sw = new StringWriter();
        new JSONWriter(sw)
            .array()
            .value(1)
            .value(2)
            .value(3)
            .endArray();
        assertEquals("[1,2,3]", sw.toString());
    }

    @Test
    public void testNestedObjectInArray() {
        StringWriter sw = new StringWriter();
        new JSONWriter(sw)
            .array()
            .object()
            .key("k")
            .value("v")
            .endObject()
            .endArray();
        assertEquals("[{\"k\":\"v\"}]", sw.toString());
    }

    @Test
    public void testNestedArrayInObject() {
        StringWriter sw = new StringWriter();
        new JSONWriter(sw)
            .object()
            .key("nums")
            .array()
            .value(1)
            .value(2)
            .endArray()
            .endObject();
        assertEquals("{\"nums\":[1,2]}", sw.toString());
    }

    @Test
    public void testMultipleKeys() {
        StringWriter sw = new StringWriter();
        new JSONWriter(sw)
            .object()
            .key("a").value(1)
            .key("b").value(2)
            .endObject();
        String result = sw.toString();
        assertTrue(result.contains("\"a\":1"));
        assertTrue(result.contains("\"b\":2"));
    }

    @Test
    public void testValueTypes() {
        StringWriter sw = new StringWriter();
        new JSONWriter(sw)
            .array()
            .value(true)
            .value(false)
            .value(42)
            .value(3.14)
            .value("text")
            .value(JSONObject.NULL)
            .endArray();
        String result = sw.toString();
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("42"));
        assertTrue(result.contains("3.14"));
        assertTrue(result.contains("\"text\""));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testMisplacedArrayThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw).object().key("k");
        // mode is 'o' after key, array is valid here
        // but calling array after endObject (mode='d') should throw
        StringWriter sw2 = new StringWriter();
        JSONWriter writer2 = new JSONWriter(sw2);
        writer2.object().key("k").value("v").endObject();
        assertThrows(JSONException.class, writer2::array);
    }

    @Test
    public void testMisplacedObjectThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw);
        writer.object().key("k").value("v").endObject();
        assertThrows(JSONException.class, writer::object);
    }

    @Test
    public void testKeyWithoutObjectThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw).array();
        assertThrows(JSONException.class, () -> writer.key("k"));
    }

    @Test
    public void testNullKeyThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw).object();
        assertThrows(JSONException.class, () -> writer.key(null));
    }

    @Test
    public void testDuplicateKeyThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw).object().key("k").value(1);
        assertThrows(JSONException.class, () -> writer.key("k"));
    }

    @Test
    public void testEndArrayMisplacedThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw).object();
        assertThrows(JSONException.class, writer::endArray);
    }

    @Test
    public void testEndObjectMisplacedThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw).array();
        assertThrows(JSONException.class, writer::endObject);
    }

    @Test
    public void testValueOutOfSequenceThrows() {
        StringWriter sw = new StringWriter();
        JSONWriter writer = new JSONWriter(sw);
        // Initial mode 'i' — value should fail
        assertThrows(JSONException.class, () -> writer.value("test"));
    }

    @Test
    public void testValueToStringNull() {
        assertEquals("null", JSONWriter.valueToString(null));
    }

    @Test
    public void testValueToStringBoolean() {
        assertEquals("true", JSONWriter.valueToString(Boolean.TRUE));
        assertEquals("false", JSONWriter.valueToString(Boolean.FALSE));
    }

    @Test
    public void testValueToStringInteger() {
        assertEquals("42", JSONWriter.valueToString(42));
    }

    @Test
    public void testValueToStringDouble() {
        assertEquals("3.14", JSONWriter.valueToString(3.14));
    }

    @Test
    public void testValueToStringString() {
        assertEquals("\"hello\"", JSONWriter.valueToString("hello"));
    }

    @Test
    public void testValueToStringJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("x", 1);
        assertEquals("{\"x\":1}", JSONWriter.valueToString(obj));
    }

    @Test
    public void testValueToStringJSONArray() {
        JSONArray arr = new JSONArray("[1,2]");
        assertEquals("[1,2]", JSONWriter.valueToString(arr));
    }

    @Test
    public void testValueToStringMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "val");
        String result = JSONWriter.valueToString(map);
        assertTrue(result.contains("\"key\""));
        assertTrue(result.contains("\"val\""));
    }

    @Test
    public void testValueToStringCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        String result = JSONWriter.valueToString(list);
        assertEquals("[1,2,3]", result);
    }

    @Test
    public void testValueToStringArray() {
        int[] arr = {1, 2, 3};
        String result = JSONWriter.valueToString(arr);
        assertEquals("[1,2,3]", result);
    }

    @Test
    public void testValueToStringEnum() {
        enum Color { RED, GREEN, BLUE }
        assertEquals("\"RED\"", JSONWriter.valueToString(Color.RED));
    }

    @Test
    public void testValueToStringJSONStringImpl() {
        JSONString js = () -> "{\"custom\":true}";
        assertEquals("{\"custom\":true}", JSONWriter.valueToString(js));
    }

    @Test
    public void testValueToStringJSONStringImplReturnsNull() {
        JSONString js = () -> null;
        assertThrows(JSONException.class, () -> JSONWriter.valueToString(js));
    }

    @Test
    public void testValueToStringJSONStringImplThrows() {
        JSONString js = () -> { throw new RuntimeException("oops"); };
        assertThrows(JSONException.class, () -> JSONWriter.valueToString(js));
    }
}
