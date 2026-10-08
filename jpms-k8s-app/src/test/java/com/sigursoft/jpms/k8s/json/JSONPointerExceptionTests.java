package com.sigursoft.jpms.k8s.json;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class JSONPointerExceptionTests {

    @Test
    public void testConstructorWithMessage() {
        String message = "Pointer error";
        JSONPointerException ex = new JSONPointerException(message);
        assertEquals(message, ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Pointer error";
        Throwable cause = new RuntimeException("Root cause");
        JSONPointerException ex = new JSONPointerException(message, cause);
        assertEquals(message, ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testExtendsJSONException() {
        JSONPointerException ex = new JSONPointerException("test");
        assertTrue(ex instanceof JSONException);
    }
}
