# NL2SQL IDE: Natural Language → SQL & Java Compiler

[![CI](https://github.com/leon004/InteliJ-internal-IDE-with-ANTLR-to-generate-SQL-from-Natural-Language/actions/workflows/ci.yml/badge.svg)](https://github.com/leon004/InteliJ-internal-IDE-with-ANTLR-to-generate-SQL-from-Natural-Language/actions/workflows/ci.yml)
![Java 17](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![ANTLR 3](https://img.shields.io/badge/ANTLR-3.5-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven)

A desktop mini-IDE that **compiles a Spanish natural-language DSL into MySQL DDL and a working Java CRUD application**.
You describe a database in plain words. The compiler checks it as you type, shows the generated SQL, and can create the database on a live MySQL server in one click.

![NL2SQL IDE compiling a program into SQL](docs/images/ide-sql.png)

```text
crear empresa                      CREATE DATABASE IF NOT EXISTS empresa;
usar empresa                       USE empresa;
tabla empleado inicio        →     CREATE TABLE IF NOT EXISTS empleado (
    nombre letras                      empleado_key INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    sueldo decimal                     nombre VARCHAR(200),
    depto foranea                      sueldo DECIMAL(10,2),
fin                                    depto_key INT,
cerrar                                 FOREIGN KEY (depto_key) REFERENCES depto(depto_key)
                                   );
```

---

## Table of contents

- [Highlights](#highlights)
- [Quick start](#quick-start)
- [The language](#the-language)
- [How it works](#how-it-works)
- [Project structure](#project-structure)
- [Development guide](#development-guide)
- [Configuration](#configuration)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Authors](#authors)
- [Resumen en español](#resumen-en-español)

## Highlights

- **Real compiler pipeline.** An ANTLR 3 grammar ([`NaturalSql.g`](src/main/antlr3/io/github/leon004/nl2sql/compiler/NaturalSql.g)) generates the lexer and parser at build time. The parser builds a typed schema model, a semantic analysis pass validates it, and separate back ends emit code.
- **Two code generators.** One emits idempotent **MySQL DDL** with primary and foreign keys. The other emits a complete **Java Swing + JDBC CRUD application** (one form per table, prepared statements). A test compiles the generated Java to make sure it builds.
- **IDE experience.** Live validation as you type, error highlighting with line and column, Spanish error messages ("Se esperaba 'inicio' pero se encontró 'inico'"), line numbers, keyboard shortcuts and five FlatLaf themes.
- **Semantic checks** that a grammar can't express: duplicate tables and columns, and foreign keys that point to undeclared or later-declared tables. Errors are caught before MySQL sees them.
- **One-click export** to a MySQL server. The export runs on a background thread, and connection settings come from environment variables (no hard-coded credentials).
- **Engineering hygiene.** Maven build, runnable fat jar, a JUnit 5 suite, and a GitHub Actions pipeline that runs the generated SQL against a real MySQL 8.4 container.

| Live error highlighting | Generated Java CRUD app |
|---|---|
| ![Error highlighting](docs/images/ide-error.png) | ![Generated Java](docs/images/ide-java.png) |

## Quick start

**Prerequisites:** JDK 17+ and Maven 3.9+. MySQL 8 is optional; you only need it for "Exportar a MySQL".

```bash
git clone https://github.com/leon004/InteliJ-internal-IDE-with-ANTLR-to-generate-SQL-from-Natural-Language.git nl2sql-ide
cd nl2sql-ide
```

```bash
mvn package
```

```bash
java -jar target/nl2sql-ide-2.0.0-all.jar
```

The IDE opens with an example program loaded. Press **Compilar** (or <kbd>Ctrl</kbd>+<kbd>Enter</kbd>) to see the SQL, a schema summary and the generated `Principal.java`.

<details>
<summary>macOS with Homebrew: installing the prerequisites</summary>

```bash
brew install openjdk@17 maven mysql
```

```bash
export JAVA_HOME="$(brew --prefix openjdk@17)"
```
</details>

## The language

A program creates one database, selects it, declares any number of tables, and ends with `cerrar`.
Keywords are case-insensitive, and `#` starts a comment.

| DSL | Meaning | Generated MySQL |
|---|---|---|
| `crear <db>` | create a database | `CREATE DATABASE IF NOT EXISTS <db>;` |
| `usar <db>` | select it | `USE <db>;` |
| `tabla <t> inicio … fin` | declare a table | `CREATE TABLE IF NOT EXISTS <t> (<t>_key INT AUTO_INCREMENT PRIMARY KEY NOT NULL, …);` |
| `cerrar` | end of program | (nothing) |

Column declarations go inside `inicio … fin` as `<name> <type>`:

| Type keyword | MySQL type |
|---|---|
| `numeros` | `INT` |
| `letras` | `VARCHAR(200)` |
| `decimal` | `DECIMAL(10,2)` |
| `fecha` | `DATE` |
| `condicion` | `BOOLEAN` |
| `foranea` | `<name>_key INT` + `FOREIGN KEY (<name>_key) REFERENCES <name>(<name>_key)` |

With `foranea`, the column name is the referenced table, and that table must be declared earlier in the program.
More complete programs are in [`examples/`](examples).

## How it works

```mermaid
flowchart LR
    A[Source text] --> B[CaseInsensitiveStringStream]
    B --> C[NaturalSqlLexer<br/><i>generated by ANTLR</i>]
    C --> D[NaturalSqlParser<br/><i>generated by ANTLR</i>]
    D -->|builds| E[(Schema model<br/>Table · Column · ColumnType)]
    E --> F[SemanticValidator]
    F --> G[SqlGenerator] --> J[MySQL DDL]
    F --> H[JavaCrudGenerator] --> K[Principal.java]
    F --> I[SchemaSummary] --> L[Schema panel]
    J --> M[MySqlExporter<br/>JDBC] --> N[(MySQL)]
```

1. **Lexing.** A case-insensitive character stream feeds the ANTLR lexer. Keywords match regardless of case, and identifiers keep their original spelling. Whitespace and `#` comments go to the hidden channel.
2. **Parsing.** The grammar's actions build immutable Java `record`s (`Schema`, `Table`, `Column`) with source spans. The parser and lexer fail fast on the first error, and [`SyntaxErrors`](src/main/java/io/github/leon004/nl2sql/compiler/SyntaxErrors.java) translates ANTLR exceptions into readable messages with exact character offsets.
3. **Semantic analysis.** [`SemanticValidator`](src/main/java/io/github/leon004/nl2sql/compiler/SemanticValidator.java) enforces unique names and foreign-key ordering, so the SQL is valid for MySQL.
4. **Code generation.** Back ends are plain classes that take a `Schema` and know nothing about the parser or the UI. You can add a new target (PostgreSQL, JPA entities, …) without touching the grammar.
5. **UI.** [`MainWindow`](src/main/java/io/github/leon004/nl2sql/ui/MainWindow.java) re-parses 300 ms after you stop typing, highlights the error span, and runs the MySQL export in a `SwingWorker` so the UI never freezes.

## Project structure

```text
├── pom.xml                                  Maven build (ANTLR codegen, tests, fat jar)
├── examples/                                Sample programs (all compiled by ExamplesTest)
├── docs/images/                             Screenshots
├── .github/workflows/ci.yml                 Build + tests + MySQL integration test
└── src
    ├── main
    │   ├── antlr3/io/github/leon004/nl2sql/compiler/
    │   │   └── NaturalSql.g                 ★ The grammar: single source of truth for the language
    │   ├── java/io/github/leon004/nl2sql/
    │   │   ├── Main.java                    Entry point (theme + window)
    │   │   ├── model/                       Schema, Table, Column, ColumnType, SourceSpan
    │   │   ├── compiler/                    NaturalLanguageCompiler (facade), error reporting, semantic checks
    │   │   ├── codegen/                     SqlGenerator, JavaCrudGenerator, SchemaSummary
    │   │   ├── db/                          DatabaseConfig, MySqlExporter
    │   │   └── ui/                          MainWindow, LineNumberGutter
    │   └── resources/…/ejemplo.txt          Program loaded on startup
    └── test/java/…                          JUnit 5 tests (+ MySqlExporterIT)
```

`target/generated-sources/antlr3/` holds the generated `NaturalSqlLexer`/`NaturalSqlParser`. They are build output, so never edit them; change the grammar instead.

## Development guide

**Open in an IDE.** IntelliJ IDEA, Eclipse, VS Code and NetBeans all import `pom.xml` directly. Run `mvn generate-sources` once (or any build) so the IDE sees the generated parser. In IntelliJ, also mark `target/generated-sources/antlr3` as a generated source root if it isn't picked up automatically.

**Common tasks**

| Task | Command |
|---|---|
| Build and run unit tests | `mvn test` |
| Everything, including integration tests | `mvn verify` |
| Run the app from sources | `mvn compile exec:java` |
| Build the runnable jar | `mvn package` → `target/nl2sql-ide-<version>-all.jar` |

**Adding a new column type** (for example `hora` → `TIME`):

1. Add the token and an alternative to the `tipo` rule in `NaturalSql.g`.
2. Add the constant to `ColumnType` with its keyword and SQL type.
3. Add a test case in `NaturalLanguageCompilerTest` / `SqlGeneratorTest`, then run `mvn test`.

**Adding a new code generator:** create a class in `codegen/` that takes a `Schema` and returns a `String`, add a tab for it in `MainWindow#buildWorkspace`, and give it a unit test.

## Configuration

"Exportar a MySQL" and the generated `Principal.java` read their connection settings from environment variables. You can also pass them as `-D` system properties, which take precedence.

| Environment variable | System property | Default |
|---|---|---|
| `NL2SQL_DB_URL` | `nl2sql.db.url` | `jdbc:mysql://localhost:3306/` |
| `NL2SQL_DB_USER` | `nl2sql.db.user` | `root` |
| `NL2SQL_DB_PASSWORD` | `nl2sql.db.password` | *(empty)* |

```bash
NL2SQL_DB_USER=root NL2SQL_DB_PASSWORD=secret java -jar target/nl2sql-ide-2.0.0-all.jar
```

## Testing

- **Unit tests** (`mvn test`) cover the grammar (valid programs, keyword and identifier edge cases, case-insensitivity, comments), error messages and highlight positions, the semantic rules, exact SQL output, and every program in `examples/`. The Java generator test **compiles the generated `Principal.java` with `javax.tools`** to prove that it builds.
- **The integration test** (`MySqlExporterIT`, run by `mvn verify`) exports a schema to MySQL twice to check idempotency, inspects the created tables through JDBC metadata, and inserts rows across a foreign key. It skips itself when `NL2SQL_DB_URL` isn't set. In CI it runs against a `mysql:8.4` service container.

## Roadmap

- [ ] Syntax highlighting and autocomplete for DSL keywords
- [ ] Column constraints (`obligatorio` → `NOT NULL`, `unico` → `UNIQUE`)
- [ ] More SQL dialects (PostgreSQL, SQLite) as additional generators
- [ ] Generate JPA entities / Spring Data repositories
- [ ] Migrate the grammar to ANTLR 4 with a visitor-based back end

## Authors

- **Leonardo Calderón Sastre** ([@leon004](https://github.com/leon004))
- **Ángel Moisés Montoya Montes de Oca**

This project started as a compiler-design course project and was later modernized: Maven build, grammar-driven code generation, semantic analysis, a test suite and CI.

## Resumen en español

**NL2SQL IDE** es un mini-IDE de escritorio que compila un lenguaje natural en español (`crear`, `usar`, `tabla … inicio … fin`, `cerrar`) a **SQL para MySQL** y a una **aplicación Java CRUD**. Está construido con una gramática **ANTLR 3**, valida el programa mientras escribes, marca los errores en rojo con línea y columna, y puede crear la base de datos directamente en MySQL.

Para ejecutarlo: `mvn package` y luego `java -jar target/nl2sql-ide-2.0.0-all.jar`. La referencia completa del lenguaje está en el menú **Ayuda → Referencia del lenguaje**.
