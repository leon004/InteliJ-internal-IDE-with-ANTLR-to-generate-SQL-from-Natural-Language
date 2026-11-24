package io.github.leon004.nl2sql.model;

/** Structural keywords of the DSL in both supported languages. */
public enum Keyword {
    CREATE("crear", "create"),
    USE("usar", "use"),
    TABLE("tabla", "table"),
    BEGIN("inicio", "begin"),
    END("fin", "end"),
    CLOSE("cerrar", "close");

    private final String spanish;
    private final String english;

    Keyword(String spanish, String english) {
        this.spanish = spanish;
        this.english = english;
    }

    public String spanish() {
        return spanish;
    }

    public String english() {
        return english;
    }

    public String in(Language language) {
        return language.pick(spanish, english);
    }
}
