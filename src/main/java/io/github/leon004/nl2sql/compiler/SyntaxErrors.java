package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.ColumnType;
import io.github.leon004.nl2sql.model.Keyword;
import io.github.leon004.nl2sql.model.Language;
import io.github.leon004.nl2sql.model.SourceSpan;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

/** Turns ANTLR recognition errors into readable, localized {@link CompilationException}s. */
final class SyntaxErrors {

    private SyntaxErrors() {
    }

    static CompilationException fromParser(RecognitionException e, NaturalSqlParser parser) {
        Language lang = parser.getLanguage();
        Token found = e.token;
        String foundText = describeToken(found, lang);
        String message;

        if (e instanceof MismatchedTokenException mte && mte.expecting > 0) {
            String expected = describeTokenType(mte.expecting, parser.getTokenNames(), lang);
            message = lang.pick(
                    "Se esperaba " + expected + " pero se encontró " + foundText + ".",
                    "Expected " + expected + " but found " + foundText + ".");
        } else if (e instanceof NoViableAltException) {
            String valid = Arrays.stream(ColumnType.values())
                    .map(t -> t.keyword(lang))
                    .collect(Collectors.joining(", "));
            message = lang.pick(
                    "Tipo de dato no válido: " + foundText + ". Usa uno de: " + valid + ".",
                    "Invalid data type: " + foundText + ". Use one of: " + valid + ".");
        } else {
            message = lang.pick("Sintaxis inválida cerca de " + foundText + ".",
                    "Invalid syntax near " + foundText + ".");
        }
        return new CompilationException(message, span(found, found), lang, e);
    }

    static CompilationException fromLexer(RecognitionException e, NaturalSqlLexer lexer) {
        Language lang = lexer.getLanguage();
        String ch = e.c == Token.EOF ? lang.pick("fin del texto", "end of input") : "'" + (char) e.c + "'";
        SourceSpan span = new SourceSpan(e.line, e.charPositionInLine, e.index, e.index + 1);
        return new CompilationException(
                lang.pick("Carácter no reconocido: ", "Unrecognized character: ") + ch + ".", span, lang, e);
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

    private static String describeToken(Token token, Language lang) {
        if (token == null || token.getType() == Token.EOF) {
            return lang.pick("el fin del texto", "the end of input");
        }
        return "'" + token.getText() + "'";
    }

    private static String describeTokenType(int type, String[] tokenNames, Language lang) {
        if (type == Token.EOF) {
            return lang.pick("el fin del texto", "the end of input");
        }
        String name = type < tokenNames.length ? tokenNames[type] : "?";
        return switch (name) {
            case "ID" -> lang.pick("un nombre", "a name");
            case "NUMERICO", "ALFABETICO", "FECHA", "DECIMAL", "CONDICION", "FORANEA" ->
                    lang.pick("un tipo de dato", "a data type");
            case "CREAR" -> quote(Keyword.CREATE, lang);
            case "USAR" -> quote(Keyword.USE, lang);
            case "TABLA" -> quote(Keyword.TABLE, lang);
            case "INICIO" -> quote(Keyword.BEGIN, lang);
            case "FIN" -> quote(Keyword.END, lang);
            case "CERRAR" -> quote(Keyword.CLOSE, lang);
            default -> "'" + name.toLowerCase() + "'";
        };
    }

    private static String quote(Keyword keyword, Language lang) {
        return "'" + keyword.in(lang) + "'";
    }
}
