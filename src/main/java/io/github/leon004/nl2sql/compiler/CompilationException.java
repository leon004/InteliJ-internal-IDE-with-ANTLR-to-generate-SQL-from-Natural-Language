package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.Language;
import io.github.leon004.nl2sql.model.SourceSpan;

/**
 * A syntax or semantic error in a program, with the location of the offending text so the
 * editor can highlight it. The message is in the program's detected language.
 */
public class CompilationException extends RuntimeException {

    private final SourceSpan span;
    private final Language language;

    public CompilationException(String message, SourceSpan span, Language language) {
        this(message, span, language, null);
    }

    public CompilationException(String message, SourceSpan span, Language language, Throwable cause) {
        super(message, cause);
        this.span = span == null ? SourceSpan.UNKNOWN : span;
        this.language = language == null ? Language.SPANISH : language;
    }

    public SourceSpan span() {
        return span;
    }

    public Language language() {
        return language;
    }

    /** Message prefixed with the line and column, e.g. {@code "Line 3:4 — ..."}. */
    public String describe() {
        if (span.line() <= 0) {
            return getMessage();
        }
        return language.pick("Línea ", "Line ") + span.line() + ":" + (span.column() + 1) + " — " + getMessage();
    }
}
