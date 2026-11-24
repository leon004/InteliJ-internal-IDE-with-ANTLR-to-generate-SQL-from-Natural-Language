package io.github.leon004.nl2sql.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leon004.nl2sql.compiler.NaturalLanguageCompiler;
import io.github.leon004.nl2sql.model.Schema;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaCrudGeneratorTest {

    private static final Schema SCHEMA = NaturalLanguageCompiler.parse("""
            crear empresa usar empresa
            tabla depto inicio nombre letras fin
            tabla vacia inicio fin
            tabla empleado inicio nombre letras sueldo decimal depto foranea fin
            cerrar""");

    @Test
    void generatedProgramCompiles(@TempDir Path dir) throws IOException {
        Path source = dir.resolve(JavaCrudGenerator.CLASS_NAME + ".java");
        Files.writeString(source, new JavaCrudGenerator().generate(SCHEMA));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run on a JDK");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exit = compiler.run(null, null, errors, "-d", dir.toString(), source.toString());

        assertEquals(0, exit, errors.toString());
    }

    @Test
    void insertsUseForeignKeyColumnNamesAndPlaceholders() {
        String code = new JavaCrudGenerator().generate(SCHEMA);

        assertTrue(code.contains("INSERT INTO empleado (nombre, sueldo, depto_key) VALUES (?, ?, ?)"), code);
        assertTrue(code.contains("INSERT INTO vacia () VALUES ()"), code);
        assertTrue(code.contains("DATABASE = \"empresa\""), code);
    }
}
