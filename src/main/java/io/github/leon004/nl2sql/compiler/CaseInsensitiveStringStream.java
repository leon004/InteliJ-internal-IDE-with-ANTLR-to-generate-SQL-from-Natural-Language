package io.github.leon004.nl2sql.compiler;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;

/**
 * Character stream that shows the lexer lower-cased characters, so keywords such as
 * {@code Tabla} or {@code INICIO} are recognised, while token text keeps its original casing.
 */
final class CaseInsensitiveStringStream extends ANTLRStringStream {

    CaseInsensitiveStringStream(String input) {
        super(input);
    }

    @Override
    public int LA(int i) {
        int c = super.LA(i);
        return c == CharStream.EOF ? c : Character.toLowerCase(c);
    }
}
