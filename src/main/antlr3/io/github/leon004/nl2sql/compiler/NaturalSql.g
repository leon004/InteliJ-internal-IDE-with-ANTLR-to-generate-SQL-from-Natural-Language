/*
 * NaturalSql.g: grammar for a small bilingual (Spanish/English) natural-language DSL that
 * describes a relational schema. Both languages are always accepted and can be mixed.
 * The parser builds a Schema model; SQL and Java code are generated from that model by the
 * classes in io.github.leon004.nl2sql.codegen.
 *
 *   crear empresa            -> CREATE DATABASE empresa
 *   usar empresa             -> USE empresa
 *   tabla depto inicio       -> CREATE TABLE depto (depto_key INT AUTO_INCREMENT PRIMARY KEY ...
 *       nombre letras        ->   nombre VARCHAR(200)
 *   fin                      -> )
 *   cerrar                   -> end of program
 *
 * English equivalents: create, use, table, begin, end, close; column types
 * number, text, date, decimal, boolean, foreign.
 *
 * Keywords are matched case-insensitively (see CaseInsensitiveStringStream).
 */
grammar NaturalSql;

options {
    language = Java;
}

@header {
package io.github.leon004.nl2sql.compiler;

import java.util.ArrayList;
import java.util.List;
import io.github.leon004.nl2sql.model.*;
}

@lexer::header {
package io.github.leon004.nl2sql.compiler;

import io.github.leon004.nl2sql.model.Language;
}

@members {
    private Language language = Language.SPANISH;

    /** Language used for error messages and for the resulting Schema. */
    public void setLanguage(Language language) {
        this.language = language;
    }

    public Language getLanguage() {
        return language;
    }

    /** Fail fast on the first syntax error instead of ANTLR's default print-and-recover. */
    @Override
    public void reportError(RecognitionException e) {
        throw SyntaxErrors.fromParser(e, this);
    }

    private static SourceSpan span(Token start, Token stop) {
        return SyntaxErrors.span(start, stop);
    }
}

@lexer::members {
    private Language language = Language.SPANISH;

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Language getLanguage() {
        return language;
    }

    @Override
    public void reportError(RecognitionException e) {
        throw SyntaxErrors.fromLexer(e, this);
    }
}

// ---------------------------------------------------------------------------
// Parser rules
// ---------------------------------------------------------------------------

programa returns [Schema schema]
@init { List<Table> tables = new ArrayList<Table>(); }
    : db=creacion use=usar (t=tabla { tables.add($t.table); })* CERRAR EOF
      { $schema = new Schema($db.name, $use.name, tables, language); }
    ;

creacion returns [String name]
    : CREAR ID { $name = $ID.text; }
    ;

usar returns [String name]
    : USAR ID { $name = $ID.text; }
    ;

tabla returns [Table table]
@init { List<Column> columns = new ArrayList<Column>(); }
    : TABLA ID INICIO (c=campo { columns.add($c.column); })* FIN
      { $table = new Table($ID.text, columns, span($ID, $ID)); }
    ;

campo returns [Column column]
    : ID tipo { $column = new Column($ID.text, $tipo.type, span($ID, $tipo.stop)); }
    ;

tipo returns [ColumnType type]
    : NUMERICO   { $type = ColumnType.NUMBER; }
    | ALFABETICO { $type = ColumnType.TEXT; }
    | FECHA      { $type = ColumnType.DATE; }
    | DECIMAL    { $type = ColumnType.DECIMAL; }
    | CONDICION  { $type = ColumnType.BOOLEAN; }
    | FORANEA    { $type = ColumnType.FOREIGN_KEY; }
    ;

// ---------------------------------------------------------------------------
// Lexer rules: each keyword accepts its Spanish and English spelling.
// Keywords are listed before ID so they take precedence.
// ---------------------------------------------------------------------------

CREAR      : 'crear'     | 'create';
USAR       : 'usar'      | 'use';
TABLA      : 'tabla'     | 'table';
INICIO     : 'inicio'    | 'begin';
FIN        : 'fin'       | 'end';
CERRAR     : 'cerrar'    | 'close';

NUMERICO   : 'numeros'   | 'number';
ALFABETICO : 'letras'    | 'text';
FECHA      : 'fecha'     | 'date';
DECIMAL    : 'decimal';
CONDICION  : 'condicion' | 'boolean';
FORANEA    : 'foranea'   | 'foreign';

ID         : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')*;

COMMENT    : '#' ~('\n' | '\r')* { $channel = HIDDEN; };

WS         : (' ' | '\t' | '\r' | '\n')+ { $channel = HIDDEN; };
