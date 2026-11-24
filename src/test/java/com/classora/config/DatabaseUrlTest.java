package com.classora.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseUrlTest {

    @Test
    void convertsARenderStyleUrl() {
        DatabaseUrl url = DatabaseUrl.parse("postgresql://user:p%40ss@db.example.com/classora");
        assertEquals("jdbc:postgresql://db.example.com:5432/classora", url.jdbcUrl());
        assertEquals("user", url.username());
        assertEquals("p@ss", url.password());
    }

    @Test
    void acceptsThePostgresAliasAndACustomPort() {
        assertEquals("jdbc:postgresql://h:6543/x", DatabaseUrl.parse("postgres://u:p@h:6543/x").jdbcUrl());
    }

    @Test
    void jdbcUrlsPassThrough() {
        DatabaseUrl url = DatabaseUrl.parse("jdbc:postgresql://localhost/classora");
        assertEquals("jdbc:postgresql://localhost/classora", url.jdbcUrl());
        assertNull(url.username());
    }

    @Test
    void rejectsMissingOrUnsupportedUrls() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseUrl.parse(""));
        assertThrows(IllegalArgumentException.class, () -> DatabaseUrl.parse(null));
        assertThrows(IllegalArgumentException.class, () -> DatabaseUrl.parse("mysql://u:p@h/db"));
    }

    @Test
    void hostedDatabasesGetSslButLocalOnesDoNot() {
        assertEquals("jdbc:postgresql://db.example.com:5432/c?sslmode=require",
                DatabaseUrl.parse("postgresql://u:p@db.example.com/c").withSslDefault().jdbcUrl());
        assertEquals("jdbc:postgresql://localhost:5432/c",
                DatabaseUrl.parse("postgresql://u:p@localhost/c").withSslDefault().jdbcUrl());
        assertEquals("jdbc:postgresql://db.example.com/c?sslmode=disable",
                DatabaseUrl.parse("jdbc:postgresql://db.example.com/c?sslmode=disable").withSslDefault().jdbcUrl());
        assertEquals("jdbc:postgresql://h:5432/c?x=1&sslmode=require",
                DatabaseUrl.parse("postgresql://u:p@h/c?x=1").withSslDefault().jdbcUrl());
    }

    @Test
    void nonPostgresUrlsAreLeftAlone() {
        assertEquals("jdbc:h2:mem:t", DatabaseUrl.parse("jdbc:h2:mem:t").withSslDefault().jdbcUrl());
    }
}
