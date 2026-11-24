package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.Language;
import io.github.leon004.nl2sql.model.Schema;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

/**
 * Entry point of the compiler front end: source text in, validated {@link Schema} out.
 *
 * <p>Spanish and English keywords are both accepted and can be mixed. The program's main
 * language is detected with {@link Language#detect(String)} and decides the language of error
 * messages and generated text.
 *
 * <pre>{@code
 * Schema schema = NaturalLanguageCompiler.parse("create shop use shop table customer begin name text end close");
 * String sql = new SqlGenerator().generate(schema);
 * }</pre>
 */
public final class NaturalLanguageCompiler {

    private NaturalLanguageCompiler() {
    }

    /**
     * Parses and validates a program.
     *
     * @throws CompilationException on the first syntax or semantic error
     */
    public static Schema parse(String source) {
        Language language = Language.detect(source);
        NaturalSqlLexer lexer = new NaturalSqlLexer(new CaseInsensitiveStringStream(source));
        lexer.setLanguage(language);
        NaturalSqlParser parser = new NaturalSqlParser(new CommonTokenStream(lexer));
        parser.setLanguage(language);
        Schema schema;
        try {
            schema = parser.programa();
        } catch (RecognitionException e) {
            throw SyntaxErrors.fromParser(e, parser);
        }
        SemanticValidator.validate(schema);
        return schema;
    }
}
