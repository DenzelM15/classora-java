package com.classora.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.hibernate.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Owns the EntityManagerFactory and gives services a simple way to run
 * work inside a transaction. One EntityManager per call, always closed.
 */
public class Database implements AutoCloseable {

    private final EntityManagerFactory factory;

    private Database(EntityManagerFactory factory) {
        this.factory = factory;
    }

    public static Database connect(DatabaseUrl rawUrl) {
    DatabaseUrl url = rawUrl.withSslDefault();

    Map<String, Object> props = new HashMap<>();

    props.put("jakarta.persistence.jdbc.url", url.jdbcUrl());

    if (url.username() != null) {
        props.put("jakarta.persistence.jdbc.user", url.username());
    }

    if (url.password() != null) {
        props.put("jakarta.persistence.jdbc.password", url.password());
    }

    props.put("hibernate.connection.provider_class",
            "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

    // Select the JDBC driver based on the database URL.
    if (url.jdbcUrl().startsWith("jdbc:postgresql:")) {
        props.put("hibernate.hikari.driverClassName",
                "org.postgresql.Driver");
    } else if (url.jdbcUrl().startsWith("jdbc:h2:")) {
        props.put("hibernate.hikari.driverClassName",
                "org.h2.Driver");
    }

    props.put("hibernate.hikari.maximumPoolSize", "5");
    props.put("hibernate.hikari.minimumIdle", "1");

    // Timestamps are stored as UTC wall-clock time.
    props.put("hibernate.jdbc.time_zone", "UTC");
    props.put("hibernate.show_sql", "false");

    Database db = new Database(
            Persistence.createEntityManagerFactory("classora", props)
    );

    db.runSchemaScript();

    return db;
}

    /** Runs work in a transaction and returns its result. Rolls back on any exception. */
    public <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager em = factory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /** For queries only. Still wrapped in a transaction so lazy loading and locking behave. */
    public <T> T read(Function<EntityManager, T> work) {
        return inTransaction(work);
    }

    private void runSchemaScript() {
        String script = readResource("/schema.sql");
        inTransaction(em -> {
            em.unwrap(Session.class).doWork(connection -> {
                for (String sql : splitStatements(script)) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        throw new IllegalStateException("Schema statement failed: " + sql, e);
                    }
                }
            });
            return null;
        });
    }

    static String[] splitStatements(String script) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                cleaned.append(line).append('\n');
            }
        }
        return java.util.Arrays.stream(cleaned.toString().split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static String readResource(String path) {
        try (InputStream in = Database.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line).append('\n');
                }
                return text.toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }

    @Override
    public void close() {
        factory.close();
    }
}
