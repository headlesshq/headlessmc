package io.github.headlesshq.headlessmc.console.jline;

import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParsedLineAdapterTest {
    private record Line(List<String> words, int wordIndex) implements ParsedLine {
        @Override
        public String word() {
            return words.get(wordIndex);
        }

        @Override
        public int wordCursor() {
            return word().length();
        }

        @Override
        public String line() {
            return String.join(" ", words);
        }

        @Override
        public int cursor() {
            return line().length();
        }
    }

    @Test
    void theAdapterExposesTheParsedLine() {
        Line line = new Line(List.of("root", "su"), 1);
        ParsedLineAdapter adapter = new ParsedLineAdapter(line);

        assertEquals("su", adapter.word());
        assertEquals(2, adapter.wordCursor());
        assertEquals(1, adapter.wordIndex());
        assertEquals(List.of("root", "su"), adapter.words());
        assertEquals("root su", adapter.line());
        assertEquals(7, adapter.cursor());
    }

}
