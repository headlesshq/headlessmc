package io.github.headlesshq.headlessmc.api.command.picocli;


import java.util.List;

/**
 * <code>ParsedLine</code> objects are returned by the {@link Parser}
 * during completion or when accepting the line.
 * The instances should implement the {@link CompletingParsedLine}
 * interface so that escape chars and quotes can be correctly handled.
 *
 * @see Parser
 * @see CompletingParsedLine
 */
interface ParsedLine {
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