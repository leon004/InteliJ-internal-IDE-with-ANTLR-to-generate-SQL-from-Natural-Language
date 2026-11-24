package io.github.leon004.nl2sql.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.leon004.nl2sql.codegen.SqlGenerator;
import io.github.leon004.nl2sql.compiler.NaturalLanguageCompiler;
import io.github.leon004.nl2sql.model.Schema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Runs the generated SQL against a real MySQL server. Skipped unless {@code NL2SQL_DB_URL} is
 * set (CI starts a MySQL service container for it).
 */
class MySqlExporterIT {

    private final DatabaseConfig config = DatabaseConfig.fromEnvironment();

    @Test
    void exportedSchemaExistsInMySqlAndExportIsIdempotent() throws Exception {
        assumeTrue(System.getenv("NL2SQL_DB_URL") != null, "NL2SQL_DB_URL not set: no MySQL available");

        Schema schema = NaturalLanguageCompiler.parse(Files.readString(Path.of("examples/escuela.txt")));
        String sql = new SqlGenerator().generate(schema);
        MySqlExporter exporter = new MySqlExporter(config);

        exporter.export(sql);
        exporter.export(sql); // second run must not fail thanks to IF NOT EXISTS

        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
            connection.setCatalog("escuela");
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getTables("escuela", null, "%", new String[] {"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            assertEquals(List.of("alumno", "calificacion", "carrera", "materia"), tables.stream().sorted().toList());

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("INSERT INTO carrera (nombre) VALUES ('Sistemas')");
                st.executeUpdate("INSERT INTO alumno (nombre, fecha_nacimiento, carrera_key) VALUES ('Ana', '2001-05-02', 1)");
            }
        }
    }

    @AfterEach
    void dropDatabase() throws Exception {
        if (System.getenv("NL2SQL_DB_URL") == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password());
             Statement st = connection.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS escuela");
        }
    }
}
