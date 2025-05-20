package io.github.headlesshq.headlessmc.api.command.picoli;

/**
 * An extension of {@link ParsedLine} that, being aware of the quoting and escaping rules
 * of the {@link Parser} that produced it, knows if and how a completion candidate
 * should be escaped/quoted.
 *
 * @author Eric Bottard
 */
interface CompletingParsedLine extends ParsedLine {
    CharSequence escape(CharSequence candidate, boolean complete);

    int rawWordCursor();

    int rawWordLength();

}
