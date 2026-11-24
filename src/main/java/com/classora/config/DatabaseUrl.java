package com.classora.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Turns a DATABASE_URL such as
 *   postgresql://user:pass@host:5432/dbname
 * (the format Render provides) into the pieces JDBC needs.
 * A ready-made "jdbc:" URL is passed through unchanged.
 */
public record DatabaseUrl(String jdbcUrl, String username, String password) {

    public static DatabaseUrl parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is not set");
        }
        String value = raw.trim();

        if (value.startsWith("jdbc:")) {
            return new DatabaseUrl(value, null, null);
        }

        URI uri = URI.create(value.replaceFirst("^postgres://", "postgresql://"));
        String scheme = uri.getScheme();
        if (!"postgresql".equals(scheme)) {
            throw new IllegalArgumentException("Unsupported database URL scheme: " + scheme);
        }

        String user = null;
        String pass = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            user = decode(colon >= 0 ? userInfo.substring(0, colon) : userInfo);
            pass = colon >= 0 ? decode(userInfo.substring(colon + 1)) : null;
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String query = uri.getRawQuery();
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(port).append(uri.getRawPath());
        if (query != null && !query.isBlank()) {
            jdbc.append('?').append(query);
        }
        return new DatabaseUrl(jdbc.toString(), user, pass);
    }

    /**
     * Hosted PostgreSQL (Render and similar) requires SSL; a local database does not.
     * "require" encrypts the connection without checking the certificate, like the
     * Node version's rejectUnauthorized: false. An explicit sslmode in the URL wins.
     */
    public DatabaseUrl withSslDefault() {
        if (!jdbcUrl.startsWith("jdbc:postgresql://") || jdbcUrl.contains("sslmode=")) {
            return this;
        }
        String afterScheme = jdbcUrl.substring("jdbc:postgresql://".length());
        int end = afterScheme.length();
        for (char stop : new char[] {':', '/', '?'}) {
            int index = afterScheme.indexOf(stop);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        String host = afterScheme.substring(0, end);
        if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]")) {
            return this;
        }
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        return new DatabaseUrl(jdbcUrl + separator + "sslmode=require", username, password);
    }

    private static String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }
}
