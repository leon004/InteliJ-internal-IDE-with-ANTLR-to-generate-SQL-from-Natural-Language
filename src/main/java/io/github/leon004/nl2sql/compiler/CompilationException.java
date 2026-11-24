package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.SourceSpan;

/**
 * A syntax or semantic error in a program, with the location of the offending text so the
 * editor can highlight it.
 */
public class CompilationException extends RuntimeException {

    private final SourceSpan span;

    public CompilationException(String message, SourceSpan span) {
        this(message, span, null);
    }

    public CompilationException(String message, SourceSpan span, Throwable cause) {
        super(message, cause);
        this.span = span == null ? SourceSpan.UNKNOWN : span;
    }

    public SourceSpan span() {
        return span;
    }

    /** Message prefixed with the line and column, e.g. {@code "Línea 3:4 — ..."}. */
    public String describe() {
        if (span.line() <= 0) {
            return getMessage();
        }
        return "Línea " + span.line() + ":" + (span.column() + 1) + " — " + getMessage();
    }
}
