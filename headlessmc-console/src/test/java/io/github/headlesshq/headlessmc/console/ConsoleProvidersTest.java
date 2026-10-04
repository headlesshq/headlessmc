package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.cache.CachedConsoleProvider;
import io.github.headlesshq.headlessmc.console.cache.ConsoleCachingService;
import io.github.headlesshq.headlessmc.console.input.Input;
import io.github.headlesshq.headlessmc.console.input.InputProvider;
import io.github.headlesshq.headlessmc.console.input.SystemInputProvider;
import io.github.headlesshq.headlessmc.console.output.Output;
import io.github.headlesshq.headlessmc.console.output.OutputProvider;
import io.github.headlesshq.headlessmc.console.output.Secret;
import io.github.headlesshq.headlessmc.console.output.StdOutOutput;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleProvidersTest {
    /** Collects written messages. */
    private static final class RecordingOutput implements Output, OutputProvider {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void write(String message) {
            messages.add(message);
        }

        @Override
        public void write(String message, String nextLine) {
            messages.add(message + nextLine);
        }

        @Override
        public Output get() {
            return this;
        }

        @Override
        public int sort() {
            return 0;
        }
    }

    /** Serves queued lines. */
    private static final class QueuedInput implements Input, InputProvider {
        private final Deque<String> lines = new ArrayDeque<>();

        @Override
        public String read() {
            return lines.poll();
        }

        @Override
        public Password readPassword() {
            return Password.of("password");
        }

        @Override
        public Input get() {
            return this;
        }

        @Override
        public int sort() {
            return 0;
        }
    }

    private static final OutputProvider UNAVAILABLE_OUTPUT = new OutputProvider() {
        @Override
        public Output get() {
            throw new ConsoleException("unavailable");
        }

        @Override
        public int sort() {
            return 0;
        }
    };

    private static final InputProvider UNAVAILABLE_INPUT = new InputProvider() {
        @Override
        public Input get() {
            throw new ConsoleException("unavailable");
        }

        @Override
        public int sort() {
            return 0;
        }
    };

    private SimpleConsoleProvider provider(List<OutputProvider> outputs, List<InputProvider> inputs) {
        return new SimpleConsoleProvider(outputs, inputs);
    }

    @Test
    void sortIsExposed() {
        SimpleConsoleProvider simple = provider(List.of(new RecordingOutput()), List.of(new QueuedInput()));

        assertEquals(ConsoleProvider.SORT_DEFAULT, simple.sort());
        assertEquals(ConsoleProvider.SORT_CACHE, new CachedConsoleProvider(new ConsoleCachingService()).sort());
    }

    @Test
    void writesGoToTheFirstAvailableOutput() {
        RecordingOutput output = new RecordingOutput();
        Console console = provider(
            List.of(UNAVAILABLE_OUTPUT, output), List.of(new QueuedInput())
        ).get();

        console.write("hello");
        console.write("a", "-b");

        assertEquals(List.of("hello", "a-b"), output.messages);
    }

    @Test
    void readsComeFromTheFirstAvailableInput() {
        QueuedInput input = new QueuedInput();
        input.lines.add("line");
        Console console = provider(
            List.of(new RecordingOutput()), List.of(UNAVAILABLE_INPUT, input)
        ).get();

        assertEquals("line", console.read());
        assertArrayEquals("password".toCharArray(), console.readPassword().get());
    }

    @Test
    void missingOutputOrInputThrowsOnUse() {
        Console noOutput = provider(List.of(), List.of(new QueuedInput())).get();
        assertThrows(ConsoleException.class, () -> noOutput.write("hello"));

        Console noInput = provider(List.of(new RecordingOutput()), List.of()).get();
        assertThrows(ConsoleException.class, noInput::read);
        assertThrows(ConsoleException.class, noInput::readPassword);
    }

    @Test
    void defaultConsoleUsesTheFirstAvailableProvider() {
        RecordingOutput output = new RecordingOutput();
        ConsoleProvider unavailable = new ConsoleProvider() {
            @Override
            public Console get() {
                throw new ConsoleException("unavailable");
            }

            @Override
            public int sort() {
                return ConsoleProvider.SORT_CACHE;
            }
        };

        Console console = DefaultConsole.create(List.of(
            provider(List.of(output), List.of(new QueuedInput())), unavailable
        ));
        console.write("hello");

        assertEquals(List.of("hello"), output.messages);
    }

    @Test
    void defaultConsoleMethods() {
        RecordingOutput output = new RecordingOutput();
        QueuedInput input = new QueuedInput();
        input.lines.add("answer");
        Console console = provider(List.of(output), List.of(input)).get();

        assertEquals("answer", console.read("question:"));
        try (Password password = console.readPassword("secret:")) {
            assertArrayEquals("password".toCharArray(), password.get());
        }

        assertEquals(Optional.empty(), console.extensions());
        assertEquals(List.of("question:", "secret:"), output.messages);
    }

    @Test
    void theDefaultCacheReturnsTheConsoleItself() {
        Console console = provider(List.of(new RecordingOutput()), List.of(new QueuedInput())).get();

        try (CachedConsole cached = console.cache()) {
            assertSame(console, cached.get());
        }
    }

    @Test
    void cachedConsolesAreServedUntilClosed() {
        ConsoleCachingService service = new ConsoleCachingService();
        CachedConsoleProvider provider = new CachedConsoleProvider(service);
        Console console = provider(List.of(new RecordingOutput()), List.of(new QueuedInput())).get();

        assertThrows(ConsoleException.class, provider::get);
        CachedConsole cached = service.cache(console);
        assertSame(console, provider.get());
        assertThrows(IllegalStateException.class, () -> service.cache(console));

        cached.close();
        assertEquals(Optional.empty(), service.get());
        assertThrows(ConsoleException.class, provider::get);
    }

    @Test
    void passwordsCannotBeUsedAfterClosing() {
        Password password = Password.of("secret");
        char[] chars = password.get();
        password.close();

        assertArrayEquals(new char[6], chars);
        assertThrows(IllegalStateException.class, password::get);
    }

    @Test
    void stdOutOutputWritesToStdOut() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        try {
            Output output = new StdOutOutput().get();
            output.write("a");
            output.write("b", "!");
        } finally {
            System.setOut(original);
        }

        assertEquals("a" + System.lineSeparator() + "b!", bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void systemInputReadsLinesFromStdIn() {
        InputStream original = System.in;
        System.setIn(new ByteArrayInputStream("hello\n".getBytes(StandardCharsets.UTF_8)));
        try {
            Input input = new SystemInputProvider().get();
            assertEquals("hello", input.read());
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void systemInputThrowsWhenStdInIsClosed() {
        InputStream original = System.in;
        System.setIn(new ByteArrayInputStream(new byte[0]));
        try {
            Input input = new SystemInputProvider().get();
            assertThrows(HeadlessMcIOException.class, input::read);
            assertThrows(HeadlessMcIOException.class, input::readPassword);
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void secretsAreNotPrinted() {
        Secret secret = new Secret("very-secret");

        assertEquals("***", secret.toString());
        assertEquals("very-secret", secret.getSecret());
    }

}
