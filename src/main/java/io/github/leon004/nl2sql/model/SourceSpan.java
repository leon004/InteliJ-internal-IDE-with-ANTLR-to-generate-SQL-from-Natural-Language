package io.github.leon004.nl2sql.model;

/**
 * Location of a construct in the source text.
 *
 * @param line         1-based line number
 * @param column       0-based column within the line
 * @param startOffset  0-based character offset of the first character (inclusive)
 * @param endOffset    0-based character offset after the last character (exclusive)
 */
public record SourceSpan(int line, int column, int startOffset, int endOffset) {

    public static final SourceSpan UNKNOWN = new SourceSpan(0, 0, -1, -1);

    public boolean isKnown() {
        return startOffset >= 0 && endOffset >= startOffset;
    }
}
