package io.github.leon004.nl2sql.model;

import java.util.Objects;

/**
 * A column declared inside a {@code tabla ... inicio ... fin} block.
 *
 * <p>For {@link ColumnType#FOREIGN_KEY} columns, {@code name} is the referenced table and the
 * physical column is {@code <name>_key}.
 */
public record Column(String name, ColumnType type, SourceSpan span) {

    public Column {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        span = span == null ? SourceSpan.UNKNOWN : span;
    }

    public Column(String name, ColumnType type) {
        this(name, type, SourceSpan.UNKNOWN);
    }

    public boolean isForeignKey() {
        return type == ColumnType.FOREIGN_KEY;
    }

    /** Name of the column as it appears in the generated SQL. */
    public String sqlName() {
        return isForeignKey() ? Table.keyColumnOf(name) : name;
    }
}
