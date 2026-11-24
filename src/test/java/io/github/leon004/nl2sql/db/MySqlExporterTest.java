package io.github.leon004.nl2sql.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class MySqlExporterTest {

    @Test
    void splitsScriptIntoStatements() {
        assertEquals(List.of("CREATE DATABASE a", "USE a", "CREATE TABLE t (\n x INT\n)"),
                MySqlExporter.splitStatements("CREATE DATABASE a;\nUSE a;\n\nCREATE TABLE t (\n x INT\n);\n"));
    }

    @Test
    void configNeverPrintsPassword() {
        assertFalse(new DatabaseConfig("jdbc:mysql://h/", "u", "s3cret").toString().contains("s3cret"));
    }
}
