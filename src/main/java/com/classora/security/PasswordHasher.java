package com.classora.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;

/**
 * BCrypt with cost 12, same as the Node version. Verification understands
 * the $2a$ / $2b$ hashes already stored in the users table.
 */
public class PasswordHasher {

    private static final int COST = 12;

    /** BCrypt only uses the first 72 bytes of a password, so longer ones are refused. */
    public static final int MAX_PASSWORD_BYTES = 72;

    public static boolean isTooLong(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }

    public String hash(String password) {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray());
    }

    public boolean matches(String password, String hash) {
        if (password == null || hash == null || isTooLong(password)) {
            return false;
        }
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}
