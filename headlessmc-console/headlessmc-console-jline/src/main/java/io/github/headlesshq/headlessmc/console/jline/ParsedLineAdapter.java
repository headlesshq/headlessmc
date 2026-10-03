package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.console.Completions;
import lombok.RequiredArgsConstructor;
import org.jline.reader.ParsedLine;

import java.util.List;

@RequiredArgsConstructor
final class ParsedLineAdapter implements Completions.Line {
    private final ParsedLine line;

    @Override
    public String word() {
        return line.word();
    }

    @Override
    public int wordCursor() {
        return line.wordCursor();
    }

    @Override
    public int wordIndex() {
        return line.wordIndex();
    }

    @Override
    public List<String> words() {
        return line.words();
    }

    @Override
    public String line() {
        return line.line();
    }

    @Override
    public int cursor() {
        return line.cursor();
    }

}
