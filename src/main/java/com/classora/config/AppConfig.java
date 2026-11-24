package com.classora.config;

import java.time.Duration;
import java.util.Map;

/** Settings read from environment variables (the same names the Node app used). */
public record AppConfig(String databaseUrl, String jwtSecret, Duration tokenLifetime) {

    public static AppConfig fromEnvironment() {
        return from(System.getenv());
    }

    public static AppConfig from(Map<String, String> env) {
        String databaseUrl = env.get("DATABASE_URL");
        String jwtSecret = env.get("JWT_SECRET");

        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL is not set");
        }
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set");
        }

        long minutes = 60;
        String configured = env.get("JWT_TTL_MINUTES");
        if (configured != null && !configured.isBlank()) {
            minutes = Long.parseLong(configured.trim());
        }
        return new AppConfig(databaseUrl, jwtSecret, Duration.ofMinutes(minutes));
    }
}
