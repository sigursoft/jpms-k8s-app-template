package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JSONPointerTests {

    @Test
    public void testQueryRootDocument() {
        JSONObject obj = new JSONObject("{\"key\":\"value\"}");
        JSONPointer pointer = new JSONPointer("");
        assertSame(obj, pointer.queryFrom(obj));
    }

    @Test
    public void testQueryHashRootDocument() {
        JSONObject obj = new JSONObject("{\"key\":\"value\"}");
        JSONPointer pointer = new JSONPointer("#");
        assertSame(obj, pointer.queryFrom(obj));
    }

    @Test
    public void testQuerySimpleKey() {
        JSONObject obj = new JSONObject("{\"key\":\"value\"}");
        JSONPointer pointer = new JSONPointer("/key");
        assertEquals("value", pointer.queryFrom(obj));
    }

    @Test
    public void testQueryNestedKey() {
        JSONObject obj = new JSONObject("{\"a\":{\"b\":\"deep\"}}");
        JSONPointer pointer = new JSONPointer("/a/b");
        assertEquals("deep", pointer.queryFrom(obj));
    }

    @Test
    public void testQueryArrayIndex() {
        JSONArray arr = new JSONArray("[10,20,30]");
        JSONPointer pointer = new JSONPointer("/1");
        assertEquals(20, pointer.queryFrom(arr));
    }

    @Test
    public void testQueryArrayIndexOutOfBounds() {
        JSONArray arr = new JSONArray("[1]");
        JSONPointer pointer = new JSONPointer("/5");
        assertThrows(JSONPointerException.class, () -> pointer.queryFrom(arr));
    }

    @Test
    public void testQueryNonNumericIndexOnArray() {
        JSONArray arr = new JSONArray("[1]");
        JSONPointer pointer = new JSONPointer("/notANumber");
        assertThrows(JSONPointerException.class, () -> pointer.queryFrom(arr));
    }

    @Test
    public void testQueryNonContainerValue() {
        JSONObject obj = new JSONObject("{\"key\":\"value\"}");
        JSONPointer pointer = new JSONPointer("/key/sub");
        assertThrows(JSONPointerException.class, () -> pointer.queryFrom(obj));
    }

    @Test
    public void testQueryFromJSONObjectUsingQueryMethod() {
        JSONObject obj = new JSONObject("{\"a\":{\"b\":42}}");
        assertEquals(42, obj.query("/a/b"));
    }

    @Test
    public void testQueryFromJSONArrayUsingQueryMethod() {
        JSONArray arr = new JSONArray("[{\"k\":\"v\"}]");
        assertEquals("v", arr.query("/0/k"));
    }

    @Test
    public void testBuilderSimple() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        JSONPointer pointer = JSONPointer.builder().append("a").build();
        assertEquals(1, pointer.queryFrom(obj));
    }

    @Test
    public void testBuilderWithIntIndex() {
        JSONArray arr = new JSONArray("[10,20]");
        JSONPointer pointer = JSONPointer.builder().append(1).build();
        assertEquals(20, pointer.queryFrom(arr));
    }

    @Test
    public void testBuilderAppendNullThrows() {
        assertThrows(NullPointerException.class,
                () -> JSONPointer.builder().append((String) null));
    }

    @Test
    public void testToString() {
        JSONPointer pointer = new JSONPointer("/a/b/c");
        assertEquals("/a/b/c", pointer.toString());
    }

    @Test
    public void testToStringEmpty() {
        JSONPointer pointer = new JSONPointer("");
        assertEquals("", pointer.toString());
    }

    @Test
    public void testToStringWithSpecialChars() {
        // Tilde must be escaped as ~0, slash as ~1
        JSONPointer pointer = JSONPointer.builder().append("a~b").append("c/d").build();
        assertEquals("/a~0b/c~1d", pointer.toString());
    }

    @Test
    public void testToURIFragment() {
        JSONPointer pointer = new JSONPointer("/a/b");
        assertEquals("#/a/b", pointer.toURIFragment());
    }

    @Test
    public void testConstructorNullThrows() {
        assertThrows(NullPointerException.class, () -> new JSONPointer((String) null));
    }

    @Test
    public void testConstructorInvalidPrefix() {
        assertThrows(IllegalArgumentException.class, () -> new JSONPointer("no/slash"));
    }

    @Test
    public void testConstructorWithHashFragment() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        JSONPointer pointer = new JSONPointer("#/a");
        assertEquals(1, pointer.queryFrom(obj));
    }

    @Test
    public void testConstructorWithListOfTokens() {
        JSONObject obj = new JSONObject("{\"a\":{\"b\":99}}");
        JSONPointer pointer = new JSONPointer(Arrays.asList("a", "b"));
        assertEquals(99, pointer.queryFrom(obj));
    }

    @Test
    public void testEscapeUnescapeRoundTrip() {
        // ~0 is ~ and ~1 is /
        JSONObject obj = new JSONObject();
        obj.put("a/b", "slash");
        obj.put("a~b", "tilde");
        assertEquals("slash", new JSONPointer("/a~1b").queryFrom(obj));
        assertEquals("tilde", new JSONPointer("/a~0b").queryFrom(obj));
    }

    @Test
    public void testConsecutiveSlashes() {
        // path with empty segment: /a//b
        JSONObject inner = new JSONObject();
        inner.put("", new JSONObject("{\"b\":5}"));
        JSONObject obj = new JSONObject();
        obj.put("a", inner);
        JSONPointer pointer = new JSONPointer("/a//b");
        assertEquals(5, pointer.queryFrom(obj));
    }

    @Test
    public void testOptQueryOnJSONObject() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        assertNull(obj.optQuery("/missing"));
        assertEquals(1, obj.optQuery("/a"));
    }

    @Test
    public void testOptQueryOnJSONArray() {
        JSONArray arr = new JSONArray("[1]");
        assertNull(arr.optQuery("/99"));
        assertEquals(1, arr.optQuery("/0"));
    }
}
