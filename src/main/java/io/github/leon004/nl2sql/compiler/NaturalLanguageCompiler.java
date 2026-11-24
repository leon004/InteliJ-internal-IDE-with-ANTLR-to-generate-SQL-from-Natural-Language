package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.Schema;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

/**
 * Entry point of the compiler front end: source text in, validated {@link Schema} out.
 *
 * <pre>{@code
 * Schema schema = NaturalLanguageCompiler.parse("crear tienda usar tienda tabla cliente inicio nombre letras fin cerrar");
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
        NaturalSqlLexer lexer = new NaturalSqlLexer(new CaseInsensitiveStringStream(source));
        NaturalSqlParser parser = new NaturalSqlParser(new CommonTokenStream(lexer));
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
