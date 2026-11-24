package io.github.leon004.nl2sql.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/** Runs a generated SQL script against a MySQL server, one statement at a time. */
public final class MySqlExporter {

    private final DatabaseConfig config;

    public MySqlExporter(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Executes every statement in {@code script} and returns how many were run.
     *
     * @throws SQLException if the connection or any statement fails
     */
    public int export(String script) throws SQLException {
        List<String> statements = splitStatements(script);
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password());
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
        return statements.size();
    }

    /** Splits on {@code ;}. Safe for generated scripts, which never contain string literals. */
    static List<String> splitStatements(String script) {
        return Arrays.stream(script.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
