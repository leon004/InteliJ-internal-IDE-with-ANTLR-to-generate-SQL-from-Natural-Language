package io.github.leon004.nl2sql.model;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Languages of the DSL. Spanish and English keywords are both always accepted and may be mixed
 * in one program; the language detected here only decides the language of error messages and
 * generated text.
 */
public enum Language {
    SPANISH,
    ENGLISH;

    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> SPANISH_WORDS = new HashSet<>();
    private static final Set<String> ENGLISH_WORDS = new HashSet<>();

    static {
        for (Keyword k : Keyword.values()) {
            SPANISH_WORDS.add(k.spanish());
            ENGLISH_WORDS.add(k.english());
        }
        for (ColumnType t : ColumnType.values()) {
            SPANISH_WORDS.add(t.spanish());
            ENGLISH_WORDS.add(t.english());
        }
        // Keywords spelled the same in both languages tell us nothing.
        Set<String> shared = new HashSet<>(SPANISH_WORDS);
        shared.retainAll(ENGLISH_WORDS);
        SPANISH_WORDS.removeAll(shared);
        ENGLISH_WORDS.removeAll(shared);
    }

    /** Picks the Spanish or English variant of a text. */
    public String pick(String spanish, String english) {
        return this == SPANISH ? spanish : english;
    }

    /**
     * Detects the main language of a program by counting language-specific keywords outside
     * comments. Ties (including programs without keywords) resolve to Spanish, unless the first
     * language-specific keyword is English.
     */
    public static Language detect(String source) {
        int spanish = 0;
        int english = 0;
        Language first = null;
        for (String line : source.split("\\R")) {
            int comment = line.indexOf('#');
            Matcher m = WORD.matcher(comment >= 0 ? line.substring(0, comment) : line);
            while (m.find()) {
                String word = m.group().toLowerCase(Locale.ROOT);
                if (SPANISH_WORDS.contains(word)) {
                    spanish++;
                    first = first == null ? SPANISH : first;
                } else if (ENGLISH_WORDS.contains(word)) {
                    english++;
                    first = first == null ? ENGLISH : first;
                }
            }
        }
        if (spanish != english) {
            return spanish > english ? SPANISH : ENGLISH;
        }
        return first == null ? SPANISH : first;
    }
}
