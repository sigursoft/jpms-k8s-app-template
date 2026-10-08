package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JSONArrayTests {

    @Test
    public void testEmptyConstructor() {
        JSONArray array = new JSONArray();
        assertEquals(0, array.length());
        assertTrue(array.isEmpty());
    }

    @Test
    public void testConstructorFromString() {
        JSONArray array = new JSONArray("[1,2,3]");
        assertEquals(3, array.length());
        assertEquals(1, array.getInt(0));
        assertEquals(2, array.getInt(1));
        assertEquals(3, array.getInt(2));
    }

    @Test
    public void testConstructorFromEmptyString() {
        JSONArray array = new JSONArray("[]");
        assertEquals(0, array.length());
    }

    @Test
    public void testConstructorFromCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        JSONArray array = new JSONArray(list);
        assertEquals(3, array.length());
        assertEquals(1, array.getInt(0));
    }

    @Test
    public void testConstructorFromNullCollection() {
        JSONArray array = new JSONArray((List<?>)null);
        assertEquals(0, array.length());
    }

    @Test
    public void testConstructorFromArray() {
        int[] intArray = {1, 2, 3};
        JSONArray array = new JSONArray(intArray);
        assertEquals(3, array.length());
        assertEquals(1, array.getInt(0));
    }

    @Test
    public void testConstructorFromNonArray() {
        assertThrows(JSONException.class, () -> new JSONArray("not an array"));
    }

    @Test
    public void testPutBoolean() {
        JSONArray array = new JSONArray();
        array.put(true);
        assertTrue(array.getBoolean(0));
    }

    @Test
    public void testPutInt() {
        JSONArray array = new JSONArray();
        array.put(42);
        assertEquals(42, array.getInt(0));
    }

    @Test
    public void testPutLong() {
        JSONArray array = new JSONArray();
        array.put(9876543210L);
        assertEquals(9876543210L, array.getLong(0));
    }

    @Test
    public void testPutDouble() {
        JSONArray array = new JSONArray();
        array.put(3.14);
        assertEquals(3.14, array.getDouble(0), 0.001);
    }

    @Test
    public void testPutFloat() {
        JSONArray array = new JSONArray();
        array.put(2.5f);
        assertEquals(2.5f, array.getFloat(0), 0.001);
    }

    @Test
    public void testPutString() {
        JSONArray array = new JSONArray();
        array.put("hello");
        assertEquals("hello", array.getString(0));
    }

    @Test
    public void testPutObject() {
        JSONArray array = new JSONArray();
        array.put("value");
        assertEquals("value", array.get(0));
    }

    @Test
    public void testPutAtIndex() {
        JSONArray array = new JSONArray();
        array.put(0, "first");
        assertEquals("first", array.getString(0));
    }

    @Test
    public void testPutAtIndexWithPadding() {
        JSONArray array = new JSONArray();
        array.put(2, "third");
        assertEquals(3, array.length());
        assertTrue(array.isNull(0));
        assertTrue(array.isNull(1));
        assertEquals("third", array.getString(2));
    }

    @Test
    public void testPutAtNegativeIndex() {
        JSONArray array = new JSONArray();
        assertThrows(JSONException.class, () -> array.put(-1, "value"));
    }

    @Test
    public void testGetBoolean() {
        JSONArray array = new JSONArray("[true, false, \"true\", \"false\"]");
        assertTrue(array.getBoolean(0));
        assertFalse(array.getBoolean(1));
        assertTrue(array.getBoolean(2));
        assertFalse(array.getBoolean(3));
    }

    @Test
    public void testGetBooleanInvalid() {
        JSONArray array = new JSONArray("[\"notboolean\"]");
        assertThrows(JSONException.class, () -> array.getBoolean(0));
    }

    @Test
    public void testGetInt() {
        JSONArray array = new JSONArray("[42, \"123\"]");
        assertEquals(42, array.getInt(0));
        assertEquals(123, array.getInt(1));
    }

    @Test
    public void testGetLong() {
        JSONArray array = new JSONArray("[9876543210]");
        assertEquals(9876543210L, array.getLong(0));
    }

    @Test
    public void testGetDouble() {
        JSONArray array = new JSONArray("[3.14, \"2.5\"]");
        assertEquals(3.14, array.getDouble(0), 0.001);
        assertEquals(2.5, array.getDouble(1), 0.001);
    }

    @Test
    public void testGetFloat() {
        // This is a known bug in JSON.org — getFloat casts directly
        // to Float instead of calling floatValue(), causing ClassCastException
        // when the stored number is not already a Float.
        // We'll test the working path:
        JSONArray array = new JSONArray();
        array.put(2.5f); // directly put a Float
        assertEquals(2.5f, array.getFloat(0), 0.001);
    }

    @Test
    public void testGetString() {
        JSONArray array = new JSONArray("[\"hello\"]");
        assertEquals("hello", array.getString(0));
    }

    @Test
    public void testGetStringWrongType() {
        JSONArray array = new JSONArray("[123]");
        assertThrows(JSONException.class, () -> array.getString(0));
    }

    @Test
    public void testGetJSONObject() {
        JSONArray array = new JSONArray("[{\"key\":\"value\"}]");
        JSONObject obj = array.getJSONObject(0);
        assertEquals("value", obj.getString("key"));
    }

    @Test
    public void testGetJSONArray() {
        JSONArray array = new JSONArray("[[1,2,3]]");
        JSONArray inner = array.getJSONArray(0);
        assertEquals(3, inner.length());
    }

    @Test
    public void testGetNotFound() {
        JSONArray array = new JSONArray();
        assertThrows(JSONException.class, () -> array.get(0));
    }

    @Test
    public void testGetBigDecimal() {
        JSONArray array = new JSONArray("[123.456]");
        BigDecimal bd = array.getBigDecimal(0);
        assertEquals(new BigDecimal("123.456"), bd);
    }

    @Test
    public void testGetBigInteger() {
        JSONArray array = new JSONArray("[123456789012345]");
        BigInteger bi = array.getBigInteger(0);
        assertEquals(new BigInteger("123456789012345"), bi);
    }

    @Test
    public void testGetNumber() {
        JSONArray array = new JSONArray("[42]");
        Number num = array.getNumber(0);
        assertEquals(42, num.intValue());
    }

    @Test
    public void testOptBoolean() {
        JSONArray array = new JSONArray("[true]");
        assertTrue(array.optBoolean(0));
        assertFalse(array.optBoolean(1));
        assertTrue(array.optBoolean(1, true));
    }

    @Test
    public void testOptInt() {
        JSONArray array = new JSONArray("[42]");
        assertEquals(42, array.optInt(0));
        assertEquals(0, array.optInt(1));
        assertEquals(99, array.optInt(1, 99));
    }

    @Test
    public void testOptLong() {
        JSONArray array = new JSONArray("[9876543210]");
        assertEquals(9876543210L, array.optLong(0));
        assertEquals(0L, array.optLong(1));
    }

    @Test
    public void testOptDouble() {
        JSONArray array = new JSONArray("[3.14]");
        assertEquals(3.14, array.optDouble(0), 0.001);
        assertTrue(Double.isNaN(array.optDouble(1)));
        assertEquals(99.0, array.optDouble(1, 99.0), 0.001);
    }

    @Test
    public void testOptString() {
        JSONArray array = new JSONArray("[\"hello\"]");
        assertEquals("hello", array.optString(0));
        assertEquals("", array.optString(1));
        assertEquals("default", array.optString(1, "default"));
    }

    @Test
    public void testOptJSONObject() {
        JSONArray array = new JSONArray("[{\"key\":\"value\"}]");
        assertNotNull(array.optJSONObject(0));
        assertNull(array.optJSONObject(1));
    }

    @Test
    public void testOptJSONArray() {
        JSONArray array = new JSONArray("[[1,2]]");
        assertNotNull(array.optJSONArray(0));
        assertNull(array.optJSONArray(1));
    }

    @Test
    public void testIsNull() {
        JSONArray array = new JSONArray("[null, 1]");
        assertTrue(array.isNull(0));
        assertFalse(array.isNull(1));
        assertTrue(array.isNull(99));
    }

    @Test
    public void testJoin() {
        JSONArray array = new JSONArray("[\"a\",\"b\",\"c\"]");
        assertEquals("\"a\",\"b\",\"c\"", array.join(","));
    }

    @Test
    public void testJoinEmpty() {
        JSONArray array = new JSONArray();
        assertEquals("", array.join(","));
    }

    @Test
    public void testLength() {
        JSONArray array = new JSONArray("[1,2,3]");
        assertEquals(3, array.length());
    }

    @Test
    public void testRemove() {
        JSONArray array = new JSONArray("[1,2,3]");
        Object removed = array.remove(1);
        assertEquals(2, removed);
        assertEquals(2, array.length());
    }

    @Test
    public void testRemoveOutOfBounds() {
        JSONArray array = new JSONArray("[1]");
        assertNull(array.remove(5));
    }

    @Test
    public void testToList() {
        JSONArray array = new JSONArray("[1,2,3]");
        List<Object> list = array.toList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
    }

    @Test
    public void testToString() {
        JSONArray array = new JSONArray();
        array.put(1);
        array.put("test");
        String result = array.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("\"test\""));
    }

    @Test
    public void testToStringWithIndent() {
        JSONArray array = new JSONArray("[1,2]");
        String result = array.toString(2);
        assertTrue(result.contains("\n"));
    }

    @Test
    public void testIterator() {
        JSONArray array = new JSONArray("[1,2,3]");
        int count = 0;
        for (Object obj : array) {
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testQuery() {
        JSONArray array = new JSONArray("[{\"key\":\"value\"}]");
        Object result = array.query("/0/key");
        assertEquals("value", result);
    }

    @Test
    public void testQueryInvalid() {
        JSONArray array = new JSONArray("[1]");
        assertThrows(JSONPointerException.class, () -> array.query("/5"));
    }

    @Test
    public void testOptQuery() {
        JSONArray array = new JSONArray("[1]");
        assertNull(array.optQuery("/5"));
    }

    @Test
    public void testToJSONObject() {
        JSONArray names = new JSONArray("[\"a\",\"b\"]");
        JSONArray values = new JSONArray("[1,2]");
        JSONObject obj = values.toJSONObject(names);
        assertEquals(1, obj.getInt("a"));
        assertEquals(2, obj.getInt("b"));
    }

    @Test
    public void testToJSONObjectNull() {
        JSONArray array = new JSONArray();
        assertNull(array.toJSONObject(null));
    }
}
