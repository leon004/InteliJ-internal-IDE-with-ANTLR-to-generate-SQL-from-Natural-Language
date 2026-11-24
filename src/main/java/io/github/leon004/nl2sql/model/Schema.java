package io.github.leon004.nl2sql.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The result of parsing a program: one database and its tables, in declaration order.
 *
 * @param databaseName the database created with {@code crear <name>}
 * @param useName      the database selected with {@code usar <name>}
 * @param tables       declared tables, in order
 */
public record Schema(String databaseName, String useName, List<Table> tables) {

    public Schema {
        Objects.requireNonNull(databaseName, "databaseName");
        Objects.requireNonNull(useName, "useName");
        tables = List.copyOf(tables);
    }

    public Optional<Table> table(String name) {
        return tables.stream().filter(t -> t.name().equals(name)).findFirst();
    }
}
