package com.classora.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @Test
    void passwordsUpTo72BytesAreAccepted() {
        assertFalse(PasswordHasher.isTooLong("a".repeat(72)));
        assertFalse(PasswordHasher.isTooLong("short"));
        assertFalse(PasswordHasher.isTooLong(null));
    }

    @Test
    void longerPasswordsAreRefused() {
        assertTrue(PasswordHasher.isTooLong("a".repeat(73)));
        // 37 two-byte characters = 74 bytes
        assertTrue(PasswordHasher.isTooLong("é".repeat(37)));
    }

    @Test
    void overlongPasswordsNeverMatchAndDoNotReachBcrypt() {
        assertFalse(new PasswordHasher().matches("a".repeat(100), "$2b$12$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234"));
    }
}
