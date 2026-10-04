package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.console.format.SimpleTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnsiTableTest {
    @Test
    void theAnsiTableRendersColumns() {
        SimpleTable<String> table = new AnsiTable<>();
        String rendered = table
            .withColumn("name", value -> value)
            .withColumn("length", value -> String.valueOf(value.length()))
            .addAll(List.of("a", "bb"))
            .toString();

        assertTrue(rendered.contains("name"));
        assertTrue(rendered.contains("bb"));
        assertTrue(rendered.contains("\n"));
    }

    @Test
    void anEmptyAnsiTableIsEmpty() {
        assertEquals("", new AnsiTable<String>().toString());
    }

}
