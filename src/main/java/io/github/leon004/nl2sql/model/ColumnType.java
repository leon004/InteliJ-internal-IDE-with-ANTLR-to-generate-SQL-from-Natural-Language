package io.github.leon004.nl2sql.model;

/**
 * Column types of the DSL, their keywords in each language, and the MySQL type each maps to.
 */
public enum ColumnType {
    NUMBER("numeros", "number", "INT"),
    TEXT("letras", "text", "VARCHAR(200)"),
    DATE("fecha", "date", "DATE"),
    DECIMAL("decimal", "decimal", "DECIMAL(10,2)"),
    BOOLEAN("condicion", "boolean", "BOOLEAN"),
    /** A reference to another table's surrogate key. */
    FOREIGN_KEY("foranea", "foreign", "INT");

    private final String spanish;
    private final String english;
    private final String sqlType;

    ColumnType(String spanish, String english, String sqlType) {
        this.spanish = spanish;
        this.english = english;
        this.sqlType = sqlType;
    }

    /** Spanish keyword, e.g. {@code letras}. */
    public String spanish() {
        return spanish;
    }

    /** English keyword, e.g. {@code text}. */
    public String english() {
        return english;
    }

    public String keyword(Language language) {
        return language.pick(spanish, english);
    }

    /** The MySQL column type, e.g. {@code VARCHAR(200)}. */
    public String sqlType() {
        return sqlType;
    }
}
