package io.github.leon004.nl2sql.model;

/**
 * Column types of the natural-language DSL and the MySQL type each one maps to.
 */
public enum ColumnType {
    NUMBER("numeros", "INT"),
    TEXT("letras", "VARCHAR(200)"),
    DATE("fecha", "DATE"),
    DECIMAL("decimal", "DECIMAL(10,2)"),
    BOOLEAN("condicion", "BOOLEAN"),
    /** A reference to another table's surrogate key. */
    FOREIGN_KEY("foranea", "INT");

    private final String keyword;
    private final String sqlType;

    ColumnType(String keyword, String sqlType) {
        this.keyword = keyword;
        this.sqlType = sqlType;
    }

    /** The Spanish keyword used in the DSL, e.g. {@code letras}. */
    public String keyword() {
        return keyword;
    }

    /** The MySQL column type, e.g. {@code VARCHAR(200)}. */
    public String sqlType() {
        return sqlType;
    }
}
