package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.Column;
import io.github.leon004.nl2sql.model.Schema;
import io.github.leon004.nl2sql.model.Table;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Checks rules the grammar cannot express, so that the generated SQL actually runs on MySQL:
 * unique table and column names, and foreign keys that point to a table declared earlier.
 */
final class SemanticValidator {

    private SemanticValidator() {
    }

    static void validate(Schema schema) {
        Set<String> declaredTables = new HashSet<>();
        for (Table table : schema.tables()) {
            if (!declaredTables.add(key(table.name()))) {
                throw new CompilationException(
                        "La tabla '" + table.name() + "' ya fue declarada.", table.span());
            }
            validateColumns(table, declaredTables);
        }
    }

    private static void validateColumns(Table table, Set<String> declaredTables) {
        Set<String> columnNames = new HashSet<>();
        columnNames.add(key(table.primaryKey()));
        for (Column column : table.columns()) {
            if (column.isForeignKey()) {
                if (column.name().equalsIgnoreCase(table.name())) {
                    throw new CompilationException(
                            "La tabla '" + table.name() + "' no puede tener una llave foránea a sí misma.",
                            column.span());
                }
                if (!declaredTables.contains(key(column.name()))) {
                    throw new CompilationException(
                            "La llave foránea '" + column.name() + "' debe apuntar a una tabla declarada antes.",
                            column.span());
                }
            }
            if (!columnNames.add(key(column.sqlName()))) {
                throw new CompilationException(
                        "La columna '" + column.sqlName() + "' está repetida en la tabla '" + table.name() + "'.",
                        column.span());
            }
        }
    }

    /** MySQL table and column names are case-insensitive on most platforms. */
    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
