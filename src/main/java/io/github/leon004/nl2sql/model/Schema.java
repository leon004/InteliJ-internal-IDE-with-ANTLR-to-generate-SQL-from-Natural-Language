package io.github.leon004.nl2sql.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The result of parsing a program: one database and its tables, in declaration order.
 *
 * @param databaseName the database created with {@code crear}/{@code create}
 * @param useName      the database selected with {@code usar}/{@code use}
 * @param tables       declared tables, in order
 * @param language     main language of the program, used for generated text
 */
public record Schema(String databaseName, String useName, List<Table> tables, Language language) {

    public Schema {
        Objects.requireNonNull(databaseName, "databaseName");
        Objects.requireNonNull(useName, "useName");
        tables = List.copyOf(tables);
        language = language == null ? Language.SPANISH : language;
    }

    public Schema(String databaseName, String useName, List<Table> tables) {
        this(databaseName, useName, tables, Language.SPANISH);
    }

    public Optional<Table> table(String name) {
        return tables.stream().filter(t -> t.name().equals(name)).findFirst();
    }
}
