package io.github.leon004.nl2sql.codegen;

import io.github.leon004.nl2sql.model.Column;
import io.github.leon004.nl2sql.model.Schema;
import io.github.leon004.nl2sql.model.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates MySQL DDL from a {@link Schema}. The script is idempotent
 * ({@code IF NOT EXISTS}) so exporting the same program twice does not fail.
 */
public final class SqlGenerator {

    public String generate(Schema schema) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE DATABASE IF NOT EXISTS ").append(schema.databaseName()).append(";\n");
        sql.append("USE ").append(schema.useName()).append(";\n");
        for (Table table : schema.tables()) {
            sql.append('\n').append(createTable(table));
        }
        return sql.toString();
    }

    String createTable(Table table) {
        List<String> lines = new ArrayList<>();
        lines.add(table.primaryKey() + " INT AUTO_INCREMENT PRIMARY KEY NOT NULL");
        for (Column column : table.columns()) {
            lines.add(column.sqlName() + " " + column.type().sqlType());
        }
        for (Column column : table.columns()) {
            if (column.isForeignKey()) {
                lines.add("FOREIGN KEY (" + column.sqlName() + ") REFERENCES "
                        + column.name() + "(" + column.sqlName() + ")");
            }
        }
        return "CREATE TABLE IF NOT EXISTS " + table.name() + " (\n    "
                + String.join(",\n    ", lines)
                + "\n);\n";
    }
}
