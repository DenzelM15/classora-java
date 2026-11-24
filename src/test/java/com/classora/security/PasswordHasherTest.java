package com.classora.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesVerifyAndDifferEachTime() {
        String first = hasher.hash("MyPassword123");
        String second = hasher.hash("MyPassword123");
        assertNotEquals(first, second);
        assertTrue(first.startsWith("$2"));
        assertTrue(hasher.matches("MyPassword123", first));
        assertFalse(hasher.matches("wrong-password", first));
    }

    @Test
    void nullsNeverMatch() {
        assertFalse(hasher.matches(null, "$2b$12$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234"));
        assertFalse(hasher.matches("x", null));
    }
}
