package io.github.headlesshq.headlessmc.progressbar.jline;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.console.ConsoleExtensions;
import io.github.headlesshq.headlessmc.console.TerminalInfo;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.InteractiveConsoleProgressBarConsumer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JLineProgressBarServiceTest {
    private final PrintStream out = new PrintStream(new ByteArrayOutputStream());

    @Test
    void testUnknownAndTinyWidthsUseDefault() {
        assertEquals(79, JLineProgressBarService.maxRenderedLength(TerminalInfo.UNKNOWN));
        assertEquals(79, JLineProgressBarService.maxRenderedLength(new TerminalInfo(9, true)));
        assertEquals(9, JLineProgressBarService.maxRenderedLength(new TerminalInfo(10, true)));
        // not clamped to the 55 columns used for picocli usage help
        assertEquals(39, JLineProgressBarService.maxRenderedLength(new TerminalInfo(40, true)));
    }

    @Test
    void testCursorMovementSelectsInteractiveConsumer() {
        FakeExtensions extensions = new FakeExtensions(new TerminalInfo(100, true));
        assertInstanceOf(InteractiveConsoleProgressBarConsumer.class,
                         JLineProgressBarService.createConsumer(extensions, out));

        extensions.info = new TerminalInfo(100, false);
        ConsoleProgressBarConsumer consumer = JLineProgressBarService.createConsumer(extensions, out);
        assertFalse(consumer instanceof InteractiveConsoleProgressBarConsumer);
    }

    @Test
    void testWidthIsQueriedOnEveryRender() {
        FakeExtensions extensions = new FakeExtensions(new TerminalInfo(100, true));
        ConsoleProgressBarConsumer consumer = JLineProgressBarService.createConsumer(extensions, out);
        assertEquals(99, consumer.getMaxRenderedLength());

        extensions.info = new TerminalInfo(60, true);
        assertEquals(59, consumer.getMaxRenderedLength());
    }

    private static final class FakeExtensions implements ConsoleExtensions {
        private volatile TerminalInfo info;

        private FakeExtensions(TerminalInfo info) {
            this.info = info;
        }

        @Override
        public TerminalInfo terminalInfo() {
            return info;
        }

        @Override
        public String edit(String initialString) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String read(String prompt, Completions completions) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getWidth() {
            throw new UnsupportedOperationException();
        }
    }

}
