/*
 * NaturalSql.g: grammar for a small Spanish natural-language DSL that describes a
 * relational schema. The parser builds a Schema model; SQL and Java code are generated
 * from that model by the classes in io.github.leon004.nl2sql.codegen.
 *
 *   crear empresa            -> CREATE DATABASE empresa
 *   usar empresa             -> USE empresa
 *   tabla depto inicio       -> CREATE TABLE depto (depto_key INT AUTO_INCREMENT PRIMARY KEY ...
 *       nombre letras        ->   nombre VARCHAR(200)
 *   fin                      -> )
 *   cerrar                   -> end of program
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
}

@members {
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
      { $schema = new Schema($db.name, $use.name, tables); }
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
// Lexer rules (keywords are listed before ID so they take precedence)
// ---------------------------------------------------------------------------

CREAR      : 'crear';
USAR       : 'usar';
TABLA      : 'tabla';
INICIO     : 'inicio';
FIN        : 'fin';
CERRAR     : 'cerrar';

NUMERICO   : 'numeros';
ALFABETICO : 'letras';
FECHA      : 'fecha';
DECIMAL    : 'decimal';
CONDICION  : 'condicion';
FORANEA    : 'foranea';

ID         : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')*;

COMMENT    : '#' ~('\n' | '\r')* { $channel = HIDDEN; };

WS         : (' ' | '\t' | '\r' | '\n')+ { $channel = HIDDEN; };
