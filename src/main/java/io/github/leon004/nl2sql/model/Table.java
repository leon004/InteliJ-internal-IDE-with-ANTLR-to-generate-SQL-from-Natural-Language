package io.github.leon004.nl2sql.model;

import java.util.List;
import java.util.Objects;

/**
 * A table declared with {@code tabla <name> inicio ... fin}. Every table gets an
 * auto-increment surrogate key named {@code <name>_key}.
 */
public record Table(String name, List<Column> columns, SourceSpan span) {

    public Table {
        Objects.requireNonNull(name, "name");
        columns = List.copyOf(columns);
        span = span == null ? SourceSpan.UNKNOWN : span;
    }

    public Table(String name, List<Column> columns) {
        this(name, columns, SourceSpan.UNKNOWN);
    }

    public String primaryKey() {
        return keyColumnOf(name);
    }

    public static String keyColumnOf(String tableName) {
        return tableName + "_key";
    }
}
