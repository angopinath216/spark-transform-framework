package com.sparktf;

import com.sparktf.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void constructorWithMessage() {
        ValidationException ex = new ValidationException("bad input");
        assertEquals("bad input", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void constructorWithMessageAndCause() {
        Throwable cause = new IllegalArgumentException("root cause");
        ValidationException ex = new ValidationException("wrapped", cause);
        assertEquals("wrapped", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void constructorWithCause() {
        Throwable cause = new RuntimeException("origin");
        ValidationException ex = new ValidationException(cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void isCheckedException() {
        assertTrue(Exception.class.isAssignableFrom(ValidationException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(ValidationException.class));
    }
}
