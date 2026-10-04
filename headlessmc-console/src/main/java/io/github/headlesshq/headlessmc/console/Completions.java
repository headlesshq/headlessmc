package io.github.headlesshq.headlessmc.console;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface Completions extends Iterable<String> {
    Iterable<Candidate> candidates(@Nullable Object spec, Line line);

    @Data
    final class Candidate {
        private final String name;
        private final @Nullable String description;
    }

    /**
     * Essentially {@code org.jline.reader.ParsedLine},
     * but we do not want a direct dependency on Jline here.
     */
    interface Line {
        /**
         * The current word being completed.
         * If the cursor is after the last word, an empty string is returned.
         *
         * @return the word being completed or an empty string
         */
        String word();

        /**
         * The cursor position within the current word.
         *
         * @return the cursor position within the current word
         */
        int wordCursor();

        /**
         * The index of the current word in the list of words.
         *
         * @return the index of the current word in the list of words
         */
        int wordIndex();

        /**
         * The list of words.
         *
         * @return the list of words
         */
        List<String> words();

        /**
         * The unparsed line.
         *
         * @return the unparsed line
         */
        String line();

        /**
         * The cursor position within the line.
         *
         * @return the cursor position within the line
         */
        int cursor();
    }

}
