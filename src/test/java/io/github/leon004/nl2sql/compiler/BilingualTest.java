package io.github.leon004.nl2sql.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leon004.nl2sql.codegen.JavaCrudGenerator;
import io.github.leon004.nl2sql.codegen.SchemaSummary;
import io.github.leon004.nl2sql.codegen.SqlGenerator;
import io.github.leon004.nl2sql.model.Column;
import io.github.leon004.nl2sql.model.ColumnType;
import io.github.leon004.nl2sql.model.Language;
import io.github.leon004.nl2sql.model.Schema;
import java.util.List;
import org.junit.jupiter.api.Test;

class BilingualTest {

    private static final String SPANISH = """
            crear empresa usar empresa
            tabla depto inicio nombre letras fin
            tabla empleado inicio edad numeros alta fecha sueldo decimal activo condicion depto foranea fin
            cerrar""";

    private static final String ENGLISH = """
            create empresa use empresa
            table depto begin nombre text end
            table empleado begin edad number alta date sueldo decimal activo boolean depto foreign end
            close""";

    @Test
    void englishProgramProducesTheSameSqlAsSpanish() {
        Schema es = NaturalLanguageCompiler.parse(SPANISH);
        Schema en = NaturalLanguageCompiler.parse(ENGLISH);

        assertEquals(Language.SPANISH, es.language());
        assertEquals(Language.ENGLISH, en.language());
        assertEquals(new SqlGenerator().generate(es), new SqlGenerator().generate(en));
    }

    @Test
    void languagesCanBeMixed() {
        Schema schema = NaturalLanguageCompiler.parse(
                "crear tienda use tienda table cliente inicio nombre text alta fecha fin close");

        assertEquals(List.of(ColumnType.TEXT, ColumnType.DATE),
                schema.tables().get(0).columns().stream().map(Column::type).toList());
    }

    @Test
    void englishKeywordsAreCaseInsensitive() {
        Schema schema = NaturalLanguageCompiler.parse("CREATE Shop Use Shop Table Customer BEGIN Name Text End Close");
        assertEquals("Customer", schema.tables().get(0).name());
    }

    @Test
    void identifiersMayStartWithEnglishKeywords() {
        Schema schema = NaturalLanguageCompiler.parse(
                "create users use users table tables begin ended date dates number user text end close");

        assertEquals("users", schema.databaseName());
        assertEquals(List.of("ended", "dates", "user"),
                schema.tables().get(0).columns().stream().map(Column::name).toList());
    }

    @Test
    void detectsMainLanguageIgnoringCommentsAndSharedWords() {
        assertEquals(Language.ENGLISH, Language.detect("# crear tabla inicio fin\ncreate x use x close"));
        assertEquals(Language.SPANISH, Language.detect("crear x usar x table t inicio fin close"));
        assertEquals(Language.ENGLISH, Language.detect("create x usar x"));    // tie: first keyword wins
        assertEquals(Language.SPANISH, Language.detect("decimal"));            // shared word only
        assertEquals(Language.SPANISH, Language.detect(""));
    }

    @Test
    void errorsUseTheProgramsLanguage() {
        String source = "create a use a\ntable t\n  name text\nend\nclose";

        CompilationException e = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(source));

        assertEquals("Expected 'begin' but found 'name'.", e.getMessage());
        assertEquals("Line 3:3 — Expected 'begin' but found 'name'.", e.describe());
        assertEquals("name", source.substring(e.span().startOffset(), e.span().endOffset()));
    }

    @Test
    void englishDataTypeErrorListsEnglishTypes() {
        CompilationException e = assertThrows(CompilationException.class,
                () -> NaturalLanguageCompiler.parse("create a use a table t begin age integer end close"));

        assertEquals("Invalid data type: 'integer'. Use one of: number, text, date, decimal, boolean, foreign.",
                e.getMessage());
    }

    @Test
    void englishSemanticAndLexerErrors() {
        CompilationException fk = assertThrows(CompilationException.class, () -> NaturalLanguageCompiler.parse(
                "create a use a table child begin parent foreign end close"));
        assertTrue(fk.getMessage().startsWith("Foreign key 'parent' must refer"), fk.getMessage());

        CompilationException ch = assertThrows(CompilationException.class,
                () -> NaturalLanguageCompiler.parse("create a use a; close"));
        assertEquals("Unrecognized character: ';'.", ch.getMessage());
    }

    @Test
    void generatedTextFollowsTheProgramsLanguage() {
        Schema en = NaturalLanguageCompiler.parse(ENGLISH);

        assertTrue(new SchemaSummary().describe(en).startsWith("Database: empresa\nTables: 2"));
        String java = new JavaCrudGenerator().generate(en);
        assertTrue(java.contains("\"Add\"") && java.contains("\"Delete all\"") && java.contains("\"Row inserted\""), java);
    }
}
