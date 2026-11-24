package io.github.leon004.nl2sql;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.leon004.nl2sql.compiler.NaturalLanguageCompiler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Every program in examples/ must compile, so the README examples never go stale. */
class ExamplesTest {

    static Stream<Path> examples() throws IOException {
        return Files.list(Path.of("examples")).filter(p -> p.toString().endsWith(".txt")).sorted();
    }

    @ParameterizedTest
    @MethodSource("examples")
    void exampleCompiles(Path example) throws IOException {
        assertFalse(NaturalLanguageCompiler.parse(Files.readString(example)).tables().isEmpty());
    }
}
