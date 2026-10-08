package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class JSONExceptionTests {

    @Test
    public void testConstructorWithMessage() {
        String message = "Test exception message";
        JSONException ex = new JSONException(message);
        assertEquals(message, ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Cause");
        JSONException ex = new JSONException(message, cause);
        assertEquals(message, ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new RuntimeException("Cause message");
        JSONException ex = new JSONException(cause);
        assertEquals("Cause message", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testIsRuntimeException() {
        JSONException ex = new JSONException("test");
        assertTrue(ex instanceof RuntimeException);
    }
}
