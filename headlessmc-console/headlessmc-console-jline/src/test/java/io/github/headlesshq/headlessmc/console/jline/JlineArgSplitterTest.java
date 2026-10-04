package io.github.headlesshq.headlessmc.console.jline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JlineArgSplitterTest {
    @Test
    void theArgSplitterSplitsQuotedArguments() {
        JlineArgSplitter splitter = new JlineArgSplitter();

        assertArrayEquals(new String[]{"a", "b"}, splitter.split("a b"));
        assertArrayEquals(new String[]{"a", "b c"}, splitter.split("a \"b c\""));
    }

}
