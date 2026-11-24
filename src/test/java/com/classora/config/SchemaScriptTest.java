package com.classora.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaScriptTest {

    @Test
    void splitsStatementsAndDropsComments() {
        String[] statements = Database.splitStatements(
                "-- a comment\nCREATE TABLE a (id INT);\n\n-- another\nALTER TABLE a ADD COLUMN b INT;\n");
        assertEquals(2, statements.length);
        assertEquals("CREATE TABLE a (id INT)", statements[0]);
        assertEquals("ALTER TABLE a ADD COLUMN b INT", statements[1]);
    }

    @Test
    void theRealSchemaFileParses() throws Exception {
        try (var in = SchemaScriptTest.class.getResourceAsStream("/schema.sql")) {
            String script = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] statements = Database.splitStatements(script);
            assertTrue(statements.length >= 8);
            for (String statement : statements) {
                assertTrue(statement.startsWith("CREATE TABLE") || statement.startsWith("ALTER TABLE"), statement);
            }
        }
    }
}
