package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class JSONTokenerTests {

    @Test
    public void testConstructorWithString() {
        JSONTokener tokener = new JSONTokener("test");
        assertEquals('t', tokener.next());
    }

    @Test
    public void testConstructorWithReader() {
        JSONTokener tokener = new JSONTokener(new StringReader("test"));
        assertEquals('t', tokener.next());
    }

    @Test
    public void testConstructorWithInputStream() {
        JSONTokener tokener = new JSONTokener(new ByteArrayInputStream("test".getBytes()));
        assertEquals('t', tokener.next());
    }

    @Test
    public void testNext() {
        JSONTokener tokener = new JSONTokener("abc");
        assertEquals('a', tokener.next());
        assertEquals('b', tokener.next());
        assertEquals('c', tokener.next());
        assertEquals(0, tokener.next()); // EOF
    }

    @Test
    public void testBack() {
        JSONTokener tokener = new JSONTokener("ab");
        assertEquals('a', tokener.next());
        tokener.back();
        assertEquals('a', tokener.next());
    }

    @Test
    public void testBackTwiceThrows() {
        JSONTokener tokener = new JSONTokener("a");
        tokener.next();
        tokener.back();
        assertThrows(JSONException.class, tokener::back);
    }

    @Test
    public void testBackAtStartThrows() {
        JSONTokener tokener = new JSONTokener("a");
        assertThrows(JSONException.class, tokener::back);
    }

    @Test
    public void testEnd() {
        JSONTokener tokener = new JSONTokener("a");
        assertFalse(tokener.end());
        tokener.next();
        tokener.next(); // EOF
        assertTrue(tokener.end());
    }

    @Test
    public void testMore() {
        JSONTokener tokener = new JSONTokener("a");
        assertTrue(tokener.more());
        tokener.next();
        assertFalse(tokener.more());
    }

    @Test
    public void testNextClean() {
        JSONTokener tokener = new JSONTokener("  \t\n  a");
        assertEquals('a', tokener.nextClean());
    }

    @Test
    public void testNextWithChar() {
        JSONTokener tokener = new JSONTokener("abc");
        assertEquals('a', tokener.next('a'));
        assertThrows(JSONException.class, () -> tokener.next('x'));
    }

    @Test
    public void testNextWithCount() {
        JSONTokener tokener = new JSONTokener("abcdef");
        assertEquals("abc", tokener.next(3));
    }

    @Test
    public void testNextWithCountZero() {
        JSONTokener tokener = new JSONTokener("abc");
        assertEquals("", tokener.next(0));
    }

    @Test
    public void testNextWithCountExceedsLength() {
        JSONTokener tokener = new JSONTokener("ab");
        assertThrows(JSONException.class, () -> tokener.next(10));
    }

    @Test
    public void testNextString() {
        JSONTokener tokener = new JSONTokener("\"hello world\"");
        tokener.next(); // skip opening quote
        assertEquals("hello world", tokener.nextString('"'));
    }

    @Test
    public void testNextStringWithEscapes() {
        JSONTokener tokener = new JSONTokener("\"line1\\nline2\\ttab\"");
        tokener.next();
        assertEquals("line1\nline2\ttab", tokener.nextString('"'));
    }

    @Test
    public void testNextStringWithUnicode() {
        JSONTokener tokener = new JSONTokener("\"\\u0041\\u0042\"");
        tokener.next();
        assertEquals("AB", tokener.nextString('"'));
    }

    @Test
    public void testNextStringUnterminated() {
        JSONTokener tokener = new JSONTokener("\"unterminated");
        tokener.next();
        assertThrows(JSONException.class, () -> tokener.nextString('"'));
    }

    @Test
    public void testNextStringIllegalEscape() {
        JSONTokener tokener = new JSONTokener("\"\\x\"");
        tokener.next();
        assertThrows(JSONException.class, () -> tokener.nextString('"'));
    }

    @Test
    public void testNextStringBadUnicode() {
        JSONTokener tokener = new JSONTokener("\"\\uXYZW\"");
        tokener.next();
        assertThrows(JSONException.class, () -> tokener.nextString('"'));
    }

    @Test
    public void testNextTo() {
        JSONTokener tokener = new JSONTokener("hello,world");
        assertEquals("hello", tokener.nextTo(','));
    }

    @Test
    public void testNextToString() {
        JSONTokener tokener = new JSONTokener("hello:=world");
        assertEquals("hello", tokener.nextTo(":="));
    }

    @Test
    public void testNextValueString() {
        JSONTokener tokener = new JSONTokener("\"value\"");
        assertEquals("value", tokener.nextValue());
    }

    @Test
    public void testNextValueNumber() {
        JSONTokener tokener = new JSONTokener("42");
        assertEquals(42, tokener.nextValue());
    }

    @Test
    public void testNextValueBoolean() {
        JSONTokener tokener = new JSONTokener("true");
        assertEquals(Boolean.TRUE, tokener.nextValue());
    }

    @Test
    public void testNextValueNull() {
        JSONTokener tokener = new JSONTokener("null");
        assertEquals(JSONObject.NULL, tokener.nextValue());
    }

    @Test
    public void testNextValueObject() {
        JSONTokener tokener = new JSONTokener("{\"key\":\"value\"}");
        Object result = tokener.nextValue();
        assertTrue(result instanceof JSONObject);
        assertEquals("value", ((JSONObject)result).getString("key"));
    }

    @Test
    public void testNextValueArray() {
        JSONTokener tokener = new JSONTokener("[1,2,3]");
        Object result = tokener.nextValue();
        assertTrue(result instanceof JSONArray);
        assertEquals(3, ((JSONArray)result).length());
    }

    @Test
    public void testDehexchar() {
        assertEquals(0, JSONTokener.dehexchar('0'));
        assertEquals(9, JSONTokener.dehexchar('9'));
        assertEquals(10, JSONTokener.dehexchar('A'));
        assertEquals(15, JSONTokener.dehexchar('F'));
        assertEquals(10, JSONTokener.dehexchar('a'));
        assertEquals(15, JSONTokener.dehexchar('f'));
        assertEquals(-1, JSONTokener.dehexchar('G'));
        assertEquals(-1, JSONTokener.dehexchar('z'));
    }

    @Test
    public void testSkipTo() {
        JSONTokener tokener = new JSONTokener("abc def");
        assertEquals(' ', tokener.skipTo(' '));
    }

    @Test
    public void testSkipToNotFound() {
        JSONTokener tokener = new JSONTokener("abc");
        assertEquals(0, tokener.skipTo('x'));
    }

    @Test
    public void testSyntaxError() {
        JSONTokener tokener = new JSONTokener("test");
        JSONException ex = tokener.syntaxError("Error occurred");
        assertTrue(ex.getMessage().contains("Error occurred"));
        assertTrue(ex.getMessage().contains("at"));
    }

    @Test
    public void testToString() {
        JSONTokener tokener = new JSONTokener("test");
        tokener.next();
        tokener.next();
        String result = tokener.toString();
        assertTrue(result.contains("at"));
        assertTrue(result.contains("character"));
        assertTrue(result.contains("line"));
    }
}
