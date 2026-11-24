package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.ColumnType;
import io.github.leon004.nl2sql.model.SourceSpan;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

/** Turns ANTLR recognition errors into user-friendly {@link CompilationException}s. */
final class SyntaxErrors {

    private static final String VALID_TYPES = Arrays.stream(ColumnType.values())
            .map(ColumnType::keyword)
            .collect(Collectors.joining(", "));

    private SyntaxErrors() {
    }

    static CompilationException fromParser(RecognitionException e, Parser parser) {
        Token found = e.token;
        String foundText = describeToken(found);
        String message;

        if (e instanceof MismatchedTokenException mte && mte.expecting > 0) {
            message = "Se esperaba " + describeTokenType(mte.expecting, parser.getTokenNames())
                    + " pero se encontró " + foundText + ".";
        } else if (e instanceof NoViableAltException) {
            message = "Tipo de dato no válido: " + foundText + ". Usa uno de: " + VALID_TYPES + ".";
        } else {
            message = "Sintaxis inválida cerca de " + foundText + ".";
        }
        return new CompilationException(message, span(found, found), e);
    }

    static CompilationException fromLexer(RecognitionException e, Lexer lexer) {
        String ch = e.c == Token.EOF ? "fin del texto" : "'" + (char) e.c + "'";
        SourceSpan span = new SourceSpan(e.line, e.charPositionInLine, e.index, e.index + 1);
        return new CompilationException("Carácter no reconocido: " + ch + ".", span, e);
    }

    static SourceSpan span(Token start, Token stop) {
        if (!(start instanceof CommonToken first) || !(stop instanceof CommonToken last)) {
            return SourceSpan.UNKNOWN;
        }
        int from = first.getStartIndex();
        int to = last.getStopIndex() + 1;
        if (first.getType() == Token.EOF) {
            // Highlight the last character so the error is still visible in the editor.
            from = Math.max(0, from - 1);
            to = from + 1;
        }
        return new SourceSpan(first.getLine(), Math.max(0, first.getCharPositionInLine()), from, Math.max(from, to));
    }

    private static String describeToken(Token token) {
        if (token == null || token.getType() == Token.EOF) {
            return "el fin del texto";
        }
        return "'" + token.getText() + "'";
    }

    private static String describeTokenType(int type, String[] tokenNames) {
        if (type == Token.EOF) {
            return "el fin del texto";
        }
        String name = type < tokenNames.length ? tokenNames[type] : "?";
        return switch (name) {
            case "ID" -> "un nombre";
            case "NUMERICO", "ALFABETICO", "FECHA", "DECIMAL", "CONDICION", "FORANEA" -> "un tipo de dato";
            default -> "'" + name.toLowerCase() + "'";
        };
    }
}
