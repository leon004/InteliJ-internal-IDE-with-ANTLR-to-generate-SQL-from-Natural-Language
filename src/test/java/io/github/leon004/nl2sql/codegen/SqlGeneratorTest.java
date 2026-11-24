package io.github.leon004.nl2sql.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leon004.nl2sql.compiler.NaturalLanguageCompiler;
import org.junit.jupiter.api.Test;

class SqlGeneratorTest {

    @Test
    void generatesMySqlDdl() {
        String sql = new SqlGenerator().generate(NaturalLanguageCompiler.parse("""
                crear empresa
                usar empresa
                tabla depto inicio
                    nombre letras
                fin
                tabla empleado inicio
                    sueldo decimal
                    alta fecha
                    activo condicion
                    edad numeros
                    depto foranea
                fin
                cerrar
                """));

        assertEquals("""
                CREATE DATABASE IF NOT EXISTS empresa;
                USE empresa;

                CREATE TABLE IF NOT EXISTS depto (
                    depto_key INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                    nombre VARCHAR(200)
                );

                CREATE TABLE IF NOT EXISTS empleado (
                    empleado_key INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                    sueldo DECIMAL(10,2),
                    alta DATE,
                    activo BOOLEAN,
                    edad INT,
                    depto_key INT,
                    FOREIGN KEY (depto_key) REFERENCES depto(depto_key)
                );
                """, sql);
    }
}
