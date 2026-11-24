package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.Column;
import io.github.leon004.nl2sql.model.Language;
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
        Language lang = schema.language();
        Set<String> declaredTables = new HashSet<>();
        for (Table table : schema.tables()) {
            if (!declaredTables.add(key(table.name()))) {
                throw new CompilationException(lang.pick(
                        "La tabla '" + table.name() + "' ya fue declarada.",
                        "Table '" + table.name() + "' is already declared."), table.span(), lang);
            }
            validateColumns(table, declaredTables, lang);
        }
    }

    private static void validateColumns(Table table, Set<String> declaredTables, Language lang) {
        Set<String> columnNames = new HashSet<>();
        columnNames.add(key(table.primaryKey()));
        for (Column column : table.columns()) {
            if (column.isForeignKey()) {
                if (column.name().equalsIgnoreCase(table.name())) {
                    throw new CompilationException(lang.pick(
                            "La tabla '" + table.name() + "' no puede tener una llave foránea a sí misma.",
                            "Table '" + table.name() + "' cannot have a foreign key to itself."),
                            column.span(), lang);
                }
                if (!declaredTables.contains(key(column.name()))) {
                    throw new CompilationException(lang.pick(
                            "La llave foránea '" + column.name() + "' debe apuntar a una tabla declarada antes.",
                            "Foreign key '" + column.name() + "' must refer to a table declared earlier."),
                            column.span(), lang);
                }
            }
            if (!columnNames.add(key(column.sqlName()))) {
                throw new CompilationException(lang.pick(
                        "La columna '" + column.sqlName() + "' está repetida en la tabla '" + table.name() + "'.",
                        "Column '" + column.sqlName() + "' is repeated in table '" + table.name() + "'."),
                        column.span(), lang);
            }
        }
    }

    /** MySQL table and column names are case-insensitive on most platforms. */
    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
