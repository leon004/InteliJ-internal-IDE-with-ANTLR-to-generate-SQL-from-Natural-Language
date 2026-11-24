package io.github.leon004.nl2sql.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leon004.nl2sql.model.Column;
import io.github.leon004.nl2sql.model.ColumnType;
import io.github.leon004.nl2sql.model.Schema;
import io.github.leon004.nl2sql.model.Table;
import java.util.List;
import org.junit.jupiter.api.Test;

class NaturalLanguageCompilerTest {

    private static final String EMPRESA = """
            crear empresa
            usar empresa
            tabla depto inicio
                nombre letras
                presupuesto decimal
            fin
            tabla empleado inicio
                nombre letras
                edad numeros
                fecha_ingreso fecha
                activo condicion
                depto foranea
            fin
            cerrar
            """;

    @Test
    void parsesDatabaseTablesAndColumns() {
        Schema schema = NaturalLanguageCompiler.parse(EMPRESA);

        assertEquals("empresa", schema.databaseName());
        assertEquals("empresa", schema.useName());
        assertEquals(List.of("depto", "empleado"), schema.tables().stream().map(Table::name).toList());

        Table empleado = schema.table("empleado").orElseThrow();
        assertEquals(List.of(ColumnType.TEXT, ColumnType.NUMBER, ColumnType.DATE, ColumnType.BOOLEAN, ColumnType.FOREIGN_KEY),
                empleado.columns().stream().map(Column::type).toList());
        assertEquals("depto_key", empleado.columns().get(4).sqlName());
    }

    @Test
    void identifiersMayStartWithAKeyword() {
        Schema schema = NaturalLanguageCompiler.parse("""
                crear tablas usar tablas
                tabla finanzas inicio fecha_alta fecha usarios numeros fin
                cerrar""");

        Table finanzas = schema.tables().get(0);
        assertEquals("finanzas", finanzas.name());
        assertEquals(List.of("fecha_alta", "usarios"), finanzas.columns().stream().map(Column::name).toList());
    }

    @Test
    void keywordsAreCaseInsensitiveButNamesKeepTheirCase() {
        Schema schema = NaturalLanguageCompiler.parse("CREAR Tienda Usar Tienda Tabla Cliente INICIO Nombre Letras Fin Cerrar");

        assertEquals("Tienda", schema.databaseName());
        assertEquals("Cliente", schema.tables().get(0).name());
        assertEquals(new Column("Nombre", ColumnType.TEXT).name(), schema.tables().get(0).columns().get(0).name());
    }

    @Test
    void ignoresComments() {
        Schema schema = NaturalLanguageCompiler.parse("# comentario\ncrear a usar a # otro\ncerrar\n");
        assertTrue(schema.tables().isEmpty());
    }

    @Test
    void reportsMissingKeywordWithLocation() {
        String source = "crear empresa\nusar empresa\ntabla depto\n  nombre letras\nfin\ncerrar";

        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(source));

        assertTrue(e.getMessage().contains("'inicio'"), e.getMessage());
        assertEquals(4, e.span().line());
        assertEquals("nombre", source.substring(e.span().startOffset(), e.span().endOffset()));
    }

    @Test
    void reportsUnknownDataType() {
        String source = "crear a usar a tabla t inicio edad entero fin cerrar";

        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(source));

        assertTrue(e.getMessage().contains("Tipo de dato no válido"), e.getMessage());
        assertEquals("entero", source.substring(e.span().startOffset(), e.span().endOffset()));
    }

    @Test
    void reportsMissingCerrarAtEndOfInput() {
        String source = "crear a usar a";

        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(source));

        assertTrue(e.getMessage().contains("'cerrar'"), e.getMessage());
        assertTrue(e.span().isKnown());
    }

    @Test
    void rejectsTextAfterCerrar() {
        assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse("crear a usar a cerrar tabla"));
    }

    @Test
    void reportsUnknownCharacter() {
        String source = "crear a usar a; cerrar";

        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(source));

        assertTrue(e.getMessage().contains("';'"), e.getMessage());
        assertEquals(source.indexOf(';'), e.span().startOffset());
    }

    @Test
    void rejectsDuplicateTable() {
        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(
                "crear a usar a tabla t inicio fin tabla T inicio fin cerrar"));
        assertTrue(e.getMessage().contains("ya fue declarada"), e.getMessage());
    }

    @Test
    void rejectsDuplicateColumn() {
        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(
                "crear a usar a tabla t inicio x letras x numeros fin cerrar"));
        assertTrue(e.getMessage().contains("repetida"), e.getMessage());
    }

    @Test
    void rejectsForeignKeyToUndeclaredTable() {
        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(
                "crear a usar a tabla hijo inicio padre foranea fin tabla padre inicio fin cerrar"));
        assertTrue(e.getMessage().contains("declarada antes"), e.getMessage());
    }
}
