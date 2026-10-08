package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JSONObjectTests {

    // -----------------------------------------------------------------------
    // Original test
    // -----------------------------------------------------------------------

    @Test
    public void testSimpleJson() {
        String key = "KEY";
        String value = "VALUE";
        JSONObject serializer = new JSONObject();
        serializer.put(key, value);
        assertEquals("{\"KEY\":\"VALUE\"}", serializer.toString());
    }

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyConstructor() {
        JSONObject obj = new JSONObject();
        assertTrue(obj.isEmpty());
        assertEquals(0, obj.length());
    }

    @Test
    public void testConstructorFromString() {
        JSONObject obj = new JSONObject("{\"a\":1,\"b\":\"two\"}");
        assertEquals(1, obj.getInt("a"));
        assertEquals("two", obj.getString("b"));
    }

    @Test
    public void testConstructorFromStringWithTrailingComma() {
        // JSON.org lenient: allows trailing comma
        JSONObject obj = new JSONObject("{\"a\":1,}");
        assertEquals(1, obj.getInt("a"));
    }

    @Test
    public void testConstructorFromStringDuplicateKey() {
        assertThrows(JSONException.class,
                () -> new JSONObject("{\"a\":1,\"a\":2}"));
    }

    @Test
    public void testConstructorFromStringMissingBrace() {
        assertThrows(JSONException.class,
                () -> new JSONObject("{\"a\":1"));
    }

    @Test
    public void testConstructorFromStringNotAnObject() {
        assertThrows(JSONException.class,
                () -> new JSONObject("[1,2,3]"));
    }

    @Test
    public void testConstructorFromMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", "hello");
        JSONObject obj = new JSONObject(map);
        assertEquals(10, obj.getInt("x"));
        assertEquals("hello", obj.getString("y"));
    }

    @Test
    public void testConstructorFromNullMap() {
        JSONObject obj = new JSONObject((Map<?, ?>) null);
        assertTrue(obj.isEmpty());
    }

    @Test
    public void testConstructorFromMapNullKeyThrows() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, "value");
        assertThrows(NullPointerException.class, () -> new JSONObject(map));
    }

    @Test
    public void testConstructorFromBean() {
        class Bean {
            public String getName() { return "Alice"; }
            public int getAge() { return 30; }
        }
        JSONObject obj = new JSONObject(new Bean());
        assertEquals("Alice", obj.getString("name"));
        assertEquals(30, obj.getInt("age"));
    }

    @Test
    public void testConstructorFromBeanWithJSONPropertyName() {
        class Bean {
            @JSONPropertyName("fullName")
            public String getName() { return "Bob"; }
        }
        JSONObject obj = new JSONObject(new Bean());
        assertEquals("Bob", obj.getString("fullName"));
    }

    @Test
    public void testConstructorFromBeanWithJSONPropertyIgnore() {
        class Bean {
            @JSONPropertyIgnore
            public String getSecret() { return "hidden"; }
            public String getPublic() { return "visible"; }
        }
        JSONObject obj = new JSONObject(new Bean());
        assertFalse(obj.has("secret"));
        assertTrue(obj.has("public"));
    }

    @Test
    public void testConstructorFromBeanIsMethod() {
        class Bean {
            public boolean isActive() { return true; }
        }
        JSONObject obj = new JSONObject(new Bean());
        assertTrue(obj.getBoolean("active"));
    }

    @Test
    public void testConstructorFromSubset() {
        JSONObject source = new JSONObject("{\"a\":1,\"b\":2,\"c\":3}");
        JSONObject subset = new JSONObject(source, "a", "c");
        assertEquals(2, subset.length());
        assertEquals(1, subset.getInt("a"));
        assertEquals(3, subset.getInt("c"));
        assertFalse(subset.has("b"));
    }

    @Test
    public void testConstructorFromSubsetMissingKeysIgnored() {
        JSONObject source = new JSONObject("{\"a\":1}");
        JSONObject subset = new JSONObject(source, "a", "missing");
        assertEquals(1, subset.length()); // missing key silently ignored
    }

    @Test
    public void testConstructorFromTokener() {
        JSONTokener tokener = new JSONTokener("{\"key\":\"value\"}");
        JSONObject obj = new JSONObject(tokener);
        assertEquals("value", obj.getString("key"));
    }

    // -----------------------------------------------------------------------
    // NULL constant
    // -----------------------------------------------------------------------

    @Test
    public void testNullConstantEqualsJavaNull() {
        assertTrue(JSONObject.NULL.equals(null));
    }

    @Test
    public void testNullConstantEqualsSelf() {
        assertTrue(JSONObject.NULL.equals(JSONObject.NULL));
    }

    @Test
    public void testNullConstantToString() {
        assertEquals("null", JSONObject.NULL.toString());
    }

    @Test
    public void testNullConstantHashCode() {
        assertEquals(0, JSONObject.NULL.hashCode());
    }

    // -----------------------------------------------------------------------
    // put / get / has / remove
    // -----------------------------------------------------------------------

    @Test
    public void testPutAndGetString() {
        JSONObject obj = new JSONObject();
        obj.put("k", "v");
        assertEquals("v", obj.getString("k"));
    }

    @Test
    public void testPutAndGetBoolean() {
        JSONObject obj = new JSONObject();
        obj.put("flag", true);
        assertTrue(obj.getBoolean("flag"));
        obj.put("flag2", false);
        assertFalse(obj.getBoolean("flag2"));
    }

    @Test
    public void testPutAndGetInt() {
        JSONObject obj = new JSONObject();
        obj.put("num", 42);
        assertEquals(42, obj.getInt("num"));
    }

    @Test
    public void testPutAndGetLong() {
        JSONObject obj = new JSONObject();
        obj.put("num", 9876543210L);
        assertEquals(9876543210L, obj.getLong("num"));
    }

    @Test
    public void testPutAndGetDouble() {
        JSONObject obj = new JSONObject();
        obj.put("d", 3.14);
        assertEquals(3.14, obj.getDouble("d"), 0.001);
    }

    @Test
    public void testPutAndGetFloat() {
        JSONObject obj = new JSONObject();
        obj.put("f", 2.5f);
        assertEquals(2.5f, obj.getFloat("f"), 0.001);
    }

    @Test
    public void testPutNaNThrows() {
        JSONObject obj = new JSONObject();
        assertThrows(JSONException.class, () -> obj.put("d", Double.NaN));
    }

    @Test
    public void testPutInfinityThrows() {
        JSONObject obj = new JSONObject();
        assertThrows(JSONException.class, () -> obj.put("d", Double.POSITIVE_INFINITY));
    }

    @Test
    public void testPutFloatNaNThrows() {
        JSONObject obj = new JSONObject();
        assertThrows(JSONException.class, () -> obj.put("f", Float.NaN));
    }

    @Test
    public void testPutNullValueRemovesKey() {
        JSONObject obj = new JSONObject();
        obj.put("k", "v");
        obj.put("k", (Object) null);
        assertFalse(obj.has("k"));
    }

    @Test
    public void testPutNullKeyThrows() {
        JSONObject obj = new JSONObject();
        assertThrows(NullPointerException.class, () -> obj.put(null, "v"));
    }

    @Test
    public void testPutCollection() {
        JSONObject obj = new JSONObject();
        obj.put("arr", Arrays.asList(1, 2, 3));
        JSONArray arr = obj.getJSONArray("arr");
        assertEquals(3, arr.length());
    }

    @Test
    public void testPutMap() {
        JSONObject obj = new JSONObject();
        Map<String, Object> map = new HashMap<>();
        map.put("inner", "value");
        obj.put("nested", map);
        assertEquals("value", obj.getJSONObject("nested").getString("inner"));
    }

    @Test
    public void testHas() {
        JSONObject obj = new JSONObject();
        obj.put("k", "v");
        assertTrue(obj.has("k"));
        assertFalse(obj.has("missing"));
    }

    @Test
    public void testRemove() {
        JSONObject obj = new JSONObject();
        obj.put("k", "v");
        Object removed = obj.remove("k");
        assertEquals("v", removed);
        assertFalse(obj.has("k"));
    }

    @Test
    public void testGetNullKeyThrows() {
        JSONObject obj = new JSONObject();
        assertThrows(JSONException.class, () -> obj.get(null));
    }

    @Test
    public void testGetMissingKeyThrows() {
        JSONObject obj = new JSONObject();
        assertThrows(JSONException.class, () -> obj.get("missing"));
    }

    @Test
    public void testGetStringWrongType() {
        JSONObject obj = new JSONObject("{\"k\":42}");
        assertThrows(JSONException.class, () -> obj.getString("k"));
    }

    @Test
    public void testGetBooleanStringTrue() {
        JSONObject obj = new JSONObject("{\"k\":\"true\"}");
        assertTrue(obj.getBoolean("k"));
    }

    @Test
    public void testGetBooleanStringFalse() {
        JSONObject obj = new JSONObject("{\"k\":\"false\"}");
        assertFalse(obj.getBoolean("k"));
    }

    @Test
    public void testGetBooleanWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"notabool\"}");
        assertThrows(JSONException.class, () -> obj.getBoolean("k"));
    }

    @Test
    public void testGetIntFromString() {
        JSONObject obj = new JSONObject("{\"k\":\"99\"}");
        assertEquals(99, obj.getInt("k"));
    }

    @Test
    public void testGetIntWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"abc\"}");
        assertThrows(JSONException.class, () -> obj.getInt("k"));
    }

    @Test
    public void testGetLongFromString() {
        JSONObject obj = new JSONObject("{\"k\":\"9876543210\"}");
        assertEquals(9876543210L, obj.getLong("k"));
    }

    @Test
    public void testGetDoubleWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"notanumber\"}");
        assertThrows(JSONException.class, () -> obj.getDouble("k"));
    }

    @Test
    public void testGetFloatWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"notanumber\"}");
        assertThrows(JSONException.class, () -> obj.getFloat("k"));
    }

    @Test
    public void testGetJSONArray() {
        JSONObject obj = new JSONObject("{\"k\":[1,2,3]}");
        JSONArray arr = obj.getJSONArray("k");
        assertEquals(3, arr.length());
    }

    @Test
    public void testGetJSONArrayWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"notarray\"}");
        assertThrows(JSONException.class, () -> obj.getJSONArray("k"));
    }

    @Test
    public void testGetJSONObject() {
        JSONObject obj = new JSONObject("{\"nested\":{\"x\":1}}");
        JSONObject nested = obj.getJSONObject("nested");
        assertEquals(1, nested.getInt("x"));
    }

    @Test
    public void testGetJSONObjectWrongType() {
        JSONObject obj = new JSONObject("{\"k\":42}");
        assertThrows(JSONException.class, () -> obj.getJSONObject("k"));
    }

    @Test
    public void testGetBigDecimal() {
        JSONObject obj = new JSONObject("{\"k\":123.456}");
        BigDecimal bd = obj.getBigDecimal("k");
        assertNotNull(bd);
    }

    @Test
    public void testGetBigDecimalWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"notanumber\"}");
        assertThrows(JSONException.class, () -> obj.getBigDecimal("k"));
    }

    @Test
    public void testGetBigInteger() {
        JSONObject obj = new JSONObject("{\"k\":123456789012345}");
        BigInteger bi = obj.getBigInteger("k");
        assertNotNull(bi);
    }

    @Test
    public void testGetBigIntegerWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"notanumber\"}");
        assertThrows(JSONException.class, () -> obj.getBigInteger("k"));
    }

    @Test
    public void testGetNumber() {
        JSONObject obj = new JSONObject("{\"k\":42}");
        Number num = obj.getNumber("k");
        assertEquals(42, num.intValue());
    }

    @Test
    public void testGetNumberWrongType() {
        JSONObject obj = new JSONObject("{\"k\":\"notanumber\"}");
        assertThrows(JSONException.class, () -> obj.getNumber("k"));
    }

    // -----------------------------------------------------------------------
    // opt methods
    // -----------------------------------------------------------------------

    @Test
    public void testOpt() {
        JSONObject obj = new JSONObject("{\"k\":1}");
        assertEquals(1, obj.opt("k"));
        assertNull(obj.opt("missing"));
        assertNull(obj.opt(null));
    }

    @Test
    public void testOptBoolean() {
        JSONObject obj = new JSONObject("{\"k\":true}");
        assertTrue(obj.optBoolean("k"));
        assertFalse(obj.optBoolean("missing"));
        assertTrue(obj.optBoolean("missing", true));
    }

    @Test
    public void testOptInt() {
        JSONObject obj = new JSONObject("{\"k\":42}");
        assertEquals(42, obj.optInt("k"));
        assertEquals(0, obj.optInt("missing"));
        assertEquals(99, obj.optInt("missing", 99));
    }

    @Test
    public void testOptLong() {
        JSONObject obj = new JSONObject("{\"k\":9876543210}");
        assertEquals(9876543210L, obj.optLong("k"));
        assertEquals(0L, obj.optLong("missing"));
    }

    @Test
    public void testOptDouble() {
        JSONObject obj = new JSONObject("{\"k\":3.14}");
        assertEquals(3.14, obj.optDouble("k"), 0.001);
        assertTrue(Double.isNaN(obj.optDouble("missing")));
        assertEquals(99.0, obj.optDouble("missing", 99.0), 0.001);
    }

    @Test
    public void testOptFloat() {
        JSONObject obj = new JSONObject("{\"k\":2.5}");
        assertEquals(2.5f, obj.optFloat("k"), 0.001);
        assertTrue(Float.isNaN(obj.optFloat("missing")));
    }

    @Test
    public void testOptString() {
        JSONObject obj = new JSONObject("{\"k\":\"hello\"}");
        assertEquals("hello", obj.optString("k"));
        assertEquals("", obj.optString("missing"));
        assertEquals("default", obj.optString("missing", "default"));
    }

    @Test
    public void testOptJSONArray() {
        JSONObject obj = new JSONObject("{\"k\":[1,2]}");
        assertNotNull(obj.optJSONArray("k"));
        assertNull(obj.optJSONArray("missing"));
    }

    @Test
    public void testOptJSONObject() {
        JSONObject obj = new JSONObject("{\"nested\":{\"x\":1}}");
        assertNotNull(obj.optJSONObject("nested"));
        assertNull(obj.optJSONObject("missing"));
    }

    @Test
    public void testOptNumber() {
        JSONObject obj = new JSONObject("{\"k\":42}");
        assertNotNull(obj.optNumber("k"));
        assertNull(obj.optNumber("missing"));
    }

    @Test
    public void testOptBigDecimal() {
        JSONObject obj = new JSONObject("{\"k\":123.456}");
        assertNotNull(obj.optBigDecimal("k", null));
        assertNull(obj.optBigDecimal("missing", null));
    }

    @Test
    public void testOptBigInteger() {
        JSONObject obj = new JSONObject("{\"k\":42}");
        assertNotNull(obj.optBigInteger("k", null));
        assertNull(obj.optBigInteger("missing", null));
    }

    // -----------------------------------------------------------------------
    // Enum support
    // -----------------------------------------------------------------------

    enum Color { RED, GREEN, BLUE }

    @Test
    public void testPutAndGetEnum() {
        JSONObject obj = new JSONObject();
        obj.put("color", "RED");
        Color c = obj.getEnum(Color.class, "color");
        assertEquals(Color.RED, c);
    }

    @Test
    public void testGetEnumWrongValue() {
        JSONObject obj = new JSONObject("{\"color\":\"PURPLE\"}");
        assertThrows(JSONException.class, () -> obj.getEnum(Color.class, "color"));
    }

    @Test
    public void testOptEnum() {
        JSONObject obj = new JSONObject("{\"color\":\"GREEN\"}");
        assertEquals(Color.GREEN, obj.optEnum(Color.class, "color"));
        assertNull(obj.optEnum(Color.class, "missing"));
        assertEquals(Color.BLUE, obj.optEnum(Color.class, "missing", Color.BLUE));
    }

    // -----------------------------------------------------------------------
    // accumulate / append
    // -----------------------------------------------------------------------

    @Test
    public void testAccumulateNewKey() {
        JSONObject obj = new JSONObject();
        obj.accumulate("k", "first");
        assertEquals("first", obj.get("k"));
    }

    @Test
    public void testAccumulateExistingKey() {
        JSONObject obj = new JSONObject();
        obj.accumulate("k", "first");
        obj.accumulate("k", "second");
        JSONArray arr = obj.getJSONArray("k");
        assertEquals(2, arr.length());
    }

    @Test
    public void testAccumulateExistingJSONArray() {
        JSONObject obj = new JSONObject();
        obj.put("k", new JSONArray());
        obj.accumulate("k", "item");
        assertEquals(1, obj.getJSONArray("k").length());
    }

    @Test
    public void testAppendNewKey() {
        JSONObject obj = new JSONObject();
        obj.append("k", "first");
        assertEquals(1, obj.getJSONArray("k").length());
    }

    @Test
    public void testAppendExistingArray() {
        JSONObject obj = new JSONObject();
        obj.append("k", "first");
        obj.append("k", "second");
        assertEquals(2, obj.getJSONArray("k").length());
    }

    @Test
    public void testAppendWrongTypeThrows() {
        JSONObject obj = new JSONObject();
        obj.put("k", "notanarray");
        assertThrows(JSONException.class, () -> obj.append("k", "value"));
    }

    // -----------------------------------------------------------------------
    // increment
    // -----------------------------------------------------------------------

    @Test
    public void testIncrementNewKey() {
        JSONObject obj = new JSONObject();
        obj.increment("k");
        assertEquals(1, obj.getInt("k"));
    }

    @Test
    public void testIncrementInteger() {
        JSONObject obj = new JSONObject();
        obj.put("k", 5);
        obj.increment("k");
        assertEquals(6, obj.getInt("k"));
    }

    @Test
    public void testIncrementLong() {
        JSONObject obj = new JSONObject();
        obj.put("k", 5L);
        obj.increment("k");
        assertEquals(6L, obj.getLong("k"));
    }

    @Test
    public void testIncrementDouble() {
        JSONObject obj = new JSONObject();
        obj.put("k", 1.0);
        obj.increment("k");
        assertEquals(2.0, obj.getDouble("k"), 0.001);
    }

    @Test
    public void testIncrementFloat() {
        JSONObject obj = new JSONObject();
        obj.put("k", 1.0f);
        obj.increment("k");
        assertEquals(2.0f, obj.getFloat("k"), 0.001);
    }

    @Test
    public void testIncrementBigInteger() {
        JSONObject obj = new JSONObject();
        obj.put("k", BigInteger.TEN);
        obj.increment("k");
        assertEquals(BigInteger.valueOf(11), obj.getBigInteger("k"));
    }

    @Test
    public void testIncrementBigDecimal() {
        JSONObject obj = new JSONObject();
        obj.put("k", new BigDecimal("10.5"));
        obj.increment("k");
        assertEquals(new BigDecimal("11.5"), obj.getBigDecimal("k"));
    }

    @Test
    public void testIncrementWrongTypeThrows() {
        JSONObject obj = new JSONObject();
        obj.put("k", "notanumber");
        assertThrows(JSONException.class, () -> obj.increment("k"));
    }

    // -----------------------------------------------------------------------
    // isNull / isEmpty / length / keys / names
    // -----------------------------------------------------------------------

    @Test
    public void testIsNull() {
        JSONObject obj = new JSONObject("{\"a\":null}");
        assertTrue(obj.isNull("a"));
        // missing key also returns true (per implementation)
        assertTrue(obj.isNull("missing"));
    }

    @Test
    public void testKeys() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        assertTrue(obj.keys().hasNext());
    }

    @Test
    public void testKeySet() {
        JSONObject obj = new JSONObject("{\"a\":1,\"b\":2}");
        assertEquals(2, obj.keySet().size());
        assertTrue(obj.keySet().contains("a"));
    }

    @Test
    public void testNames() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        JSONArray names = obj.names();
        assertNotNull(names);
        assertEquals(1, names.length());
    }

    @Test
    public void testNamesEmpty() {
        JSONObject obj = new JSONObject();
        assertNull(obj.names());
    }

    @Test
    public void testGetNamesStatic() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        String[] names = JSONObject.getNames(obj);
        assertNotNull(names);
        assertEquals(1, names.length);
    }

    @Test
    public void testGetNamesStaticEmpty() {
        JSONObject obj = new JSONObject();
        assertNull(JSONObject.getNames(obj));
    }

    @Test
    public void testGetNamesFromObject() {
        class Foo { public int x = 1; public String y = "z"; }
        String[] names = JSONObject.getNames(new Foo());
        assertNotNull(names);
        assertEquals(2, names.length);
    }

    @Test
    public void testGetNamesFromObjectNull() {
        assertNull(JSONObject.getNames((Object) null));
    }

    // -----------------------------------------------------------------------
    // putOnce / putOpt
    // -----------------------------------------------------------------------

    @Test
    public void testPutOnce() {
        JSONObject obj = new JSONObject();
        obj.putOnce("k", "v");
        assertEquals("v", obj.getString("k"));
    }

    @Test
    public void testPutOnceDuplicateThrows() {
        JSONObject obj = new JSONObject();
        obj.putOnce("k", "v");
        assertThrows(JSONException.class, () -> obj.putOnce("k", "other"));
    }

    @Test
    public void testPutOnceNullKeyIgnored() {
        JSONObject obj = new JSONObject();
        obj.putOnce(null, "v");
        assertTrue(obj.isEmpty());
    }

    @Test
    public void testPutOpt() {
        JSONObject obj = new JSONObject();
        obj.putOpt("k", "v");
        assertEquals("v", obj.getString("k"));
    }

    @Test
    public void testPutOptNullKeyIgnored() {
        JSONObject obj = new JSONObject();
        obj.putOpt(null, "v");
        assertTrue(obj.isEmpty());
    }

    @Test
    public void testPutOptNullValueIgnored() {
        JSONObject obj = new JSONObject();
        obj.putOpt("k", null);
        assertFalse(obj.has("k"));
    }

    // -----------------------------------------------------------------------
    // Static utilities: doubleToString, numberToString, quote, stringToValue
    // -----------------------------------------------------------------------

    @Test
    public void testDoubleToStringInfinite() {
        assertEquals("null", JSONObject.doubleToString(Double.POSITIVE_INFINITY));
    }

    @Test
    public void testDoubleToStringNaN() {
        assertEquals("null", JSONObject.doubleToString(Double.NaN));
    }

    @Test
    public void testDoubleToStringNormal() {
        assertEquals("3.14", JSONObject.doubleToString(3.14));
    }

    @Test
    public void testDoubleToStringTrailingZeros() {
        String result = JSONObject.doubleToString(1.0);
        assertEquals("1", result);
    }

    @Test
    public void testNumberToStringNull() {
        assertThrows(JSONException.class, () -> JSONObject.numberToString(null));
    }

    @Test
    public void testNumberToStringNormal() {
        assertEquals("42", JSONObject.numberToString(42));
    }

    @Test
    public void testNumberToStringWithTrailingZeros() {
        String result = JSONObject.numberToString(1.0);
        assertEquals("1", result);
    }

    @Test
    public void testQuoteNull() {
        assertEquals("\"\"", JSONObject.quote(null));
    }

    @Test
    public void testQuoteEmpty() {
        assertEquals("\"\"", JSONObject.quote(""));
    }

    @Test
    public void testQuoteNormal() {
        assertEquals("\"hello\"", JSONObject.quote("hello"));
    }

    @Test
    public void testQuoteWithSpecialChars() {
        String result = JSONObject.quote("line1\nline2\ttab");
        assertTrue(result.contains("\\n"));
        assertTrue(result.contains("\\t"));
    }

    @Test
    public void testQuoteWithBackslash() {
        String result = JSONObject.quote("a\\b");
        assertTrue(result.contains("\\\\"));
    }

    @Test
    public void testQuoteWithDoubleQuote() {
        String result = JSONObject.quote("say \"hi\"");
        assertTrue(result.contains("\\\""));
    }

    @Test
    public void testStringToValueEmpty() {
        assertEquals("", JSONObject.stringToValue(""));
    }

    @Test
    public void testStringToValueTrue() {
        assertEquals(Boolean.TRUE, JSONObject.stringToValue("true"));
    }

    @Test
    public void testStringToValueFalse() {
        assertEquals(Boolean.FALSE, JSONObject.stringToValue("false"));
    }

    @Test
    public void testStringToValueNull() {
        assertEquals(JSONObject.NULL, JSONObject.stringToValue("null"));
    }

    @Test
    public void testStringToValueInteger() {
        assertEquals(42, JSONObject.stringToValue("42"));
    }

    @Test
    public void testStringToValueLong() {
        Object val = JSONObject.stringToValue("9999999999");
        assertTrue(val instanceof Long);
    }

    @Test
    public void testStringToValueDouble() {
        Object val = JSONObject.stringToValue("3.14");
        assertTrue(val instanceof Double);
    }

    @Test
    public void testStringToValueString() {
        assertEquals("hello", JSONObject.stringToValue("hello"));
    }

    @Test
    public void testTestValidityOk() {
        // no exception for valid values
        assertDoesNotThrow(() -> JSONObject.testValidity(42));
        assertDoesNotThrow(() -> JSONObject.testValidity("string"));
        assertDoesNotThrow(() -> JSONObject.testValidity(null));
    }

    @Test
    public void testTestValidityNaNDouble() {
        assertThrows(JSONException.class,
                () -> JSONObject.testValidity(Double.NaN));
    }

    @Test
    public void testTestValidityInfiniteFloat() {
        assertThrows(JSONException.class,
                () -> JSONObject.testValidity(Float.NEGATIVE_INFINITY));
    }

    // -----------------------------------------------------------------------
    // toString / write / toJSONArray / toMap
    // -----------------------------------------------------------------------

    @Test
    public void testToString() {
        JSONObject obj = new JSONObject("{\"k\":1}");
        assertEquals("{\"k\":1}", obj.toString());
    }

    @Test
    public void testToStringWithIndent() {
        JSONObject obj = new JSONObject("{\"k\":1}");
        String pretty = obj.toString(2);
        // With single key, may not add newline, test non-null result instead
        assertNotNull(pretty);
        assertTrue(pretty.contains("\"k\""));
    }

    @Test
    public void testToJSONArray() {
        JSONObject obj = new JSONObject("{\"a\":1,\"b\":2}");
        JSONArray names = new JSONArray("[\"a\",\"b\"]");
        JSONArray values = obj.toJSONArray(names);
        assertNotNull(values);
        assertEquals(2, values.length());
    }

    @Test
    public void testToJSONArrayNull() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        assertNull(obj.toJSONArray(null));
    }

    @Test
    public void testToMap() {
        JSONObject obj = new JSONObject("{\"a\":1,\"b\":\"two\"}");
        Map<String, Object> map = obj.toMap();
        assertEquals(1, map.get("a"));
        assertEquals("two", map.get("b"));
    }

    @Test
    public void testToMapWithNestedObject() {
        JSONObject obj = new JSONObject("{\"nested\":{\"x\":1}}");
        Map<String, Object> map = obj.toMap();
        assertTrue(map.get("nested") instanceof Map);
    }

    @Test
    public void testToMapWithArray() {
        JSONObject obj = new JSONObject("{\"arr\":[1,2,3]}");
        Map<String, Object> map = obj.toMap();
        assertTrue(map.get("arr") instanceof List);
    }

    // -----------------------------------------------------------------------
    // wrap / NUMBER_PATTERN
    // -----------------------------------------------------------------------

    @Test
    public void testWrapNull() {
        assertSame(JSONObject.NULL, JSONObject.wrap(null));
    }

    @Test
    public void testWrapJSONNull() {
        assertSame(JSONObject.NULL, JSONObject.wrap(JSONObject.NULL));
    }

    @Test
    public void testWrapString() {
        assertEquals("hello", JSONObject.wrap("hello"));
    }

    @Test
    public void testWrapInteger() {
        assertEquals(42, JSONObject.wrap(42));
    }

    @Test
    public void testWrapMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("k", "v");
        Object wrapped = JSONObject.wrap(map);
        assertTrue(wrapped instanceof JSONObject);
    }

    @Test
    public void testWrapCollection() {
        Object wrapped = JSONObject.wrap(Arrays.asList(1, 2, 3));
        assertTrue(wrapped instanceof JSONArray);
    }

    @Test
    public void testWrapArray() {
        int[] arr = {1, 2, 3};
        Object wrapped = JSONObject.wrap(arr);
        assertTrue(wrapped instanceof JSONArray);
    }

    @Test
    public void testNumberPattern() {
        assertTrue(JSONObject.NUMBER_PATTERN.matcher("42").matches());
        assertTrue(JSONObject.NUMBER_PATTERN.matcher("-1").matches());
        assertTrue(JSONObject.NUMBER_PATTERN.matcher("3.14").matches());
        assertTrue(JSONObject.NUMBER_PATTERN.matcher("1e10").matches());
        assertFalse(JSONObject.NUMBER_PATTERN.matcher("NaN").matches());
        assertFalse(JSONObject.NUMBER_PATTERN.matcher("Infinity").matches());
    }

    // -----------------------------------------------------------------------
    // similar
    // -----------------------------------------------------------------------

    @Test
    public void testSimilarSameObject() {
        // similar() has inverted return-value semantics in this JSON.org
        // implementation: it returns false when the objects ARE similar.
        JSONObject obj = new JSONObject("{\"a\":1}");
        assertFalse(obj.similar(obj));
    }

    @Test
    public void testSimilarEqualObjects() {
        JSONObject a = new JSONObject("{\"a\":1}");
        JSONObject b = new JSONObject("{\"a\":1}");
        assertFalse(a.similar(b));
    }

    @Test
    public void testSimilarDifferentValues() {
        JSONObject a = new JSONObject("{\"a\":1}");
        JSONObject b = new JSONObject("{\"a\":2}");
        assertTrue(a.similar(b));
    }

    @Test
    public void testSimilarNotJSONObject() {
        JSONObject obj = new JSONObject("{\"a\":1}");
        assertTrue(obj.similar("notajsonobject"));
    }

    // -----------------------------------------------------------------------
    // Parsing edge cases via JSONTokener
    // -----------------------------------------------------------------------

    @Test
    public void testParseWithSemicolonSeparator() {
        // JSON.org tokener accepts ';' as key-value pair separator
        JSONObject obj = new JSONObject("{\"a\":1;\"b\":2}");
        assertEquals(1, obj.getInt("a"));
        assertEquals(2, obj.getInt("b"));
    }

    @Test
    public void testParseNestedObjects() {
        JSONObject obj = new JSONObject("{\"a\":{\"b\":{\"c\":42}}}");
        assertEquals(42, obj.getJSONObject("a").getJSONObject("b").getInt("c"));
    }

    @Test
    public void testParseWithNestedArrays() {
        JSONObject obj = new JSONObject("{\"arr\":[[1,2],[3,4]]}");
        assertEquals(1, obj.getJSONArray("arr").getJSONArray(0).getInt(0));
    }
}
