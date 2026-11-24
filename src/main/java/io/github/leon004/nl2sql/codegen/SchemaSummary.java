package io.github.leon004.nl2sql.codegen;

import io.github.leon004.nl2sql.model.Column;
import io.github.leon004.nl2sql.model.Schema;
import io.github.leon004.nl2sql.model.Table;

/** Human-readable overview of a {@link Schema} (the "Esquema" panel in the IDE). */
public final class SchemaSummary {

    public String describe(Schema schema) {
        StringBuilder out = new StringBuilder();
        out.append("Base de datos: ").append(schema.databaseName()).append('\n');
        out.append("Tablas: ").append(schema.tables().size()).append("\n");
        for (Table table : schema.tables()) {
            out.append("\n• ").append(table.name())
                    .append("  (PK ").append(table.primaryKey()).append(")\n");
            for (Column column : table.columns()) {
                out.append("    - ").append(column.sqlName()).append(" : ").append(column.type().sqlType());
                if (column.isForeignKey()) {
                    out.append("  → ").append(column.name());
                }
                out.append('\n');
            }
        }
        return out.toString();
    }
}
