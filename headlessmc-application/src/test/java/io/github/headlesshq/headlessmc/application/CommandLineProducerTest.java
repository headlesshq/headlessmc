package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLineProducerTest {
    private static int handle(RecordingConsole console, Exception exception) {
        return CommandLineProducer.handleExecutionException(console, exception, new CommandLine(
            // not an annotated class, Quarkus would register that as another top command bean
            CommandLine.Model.CommandSpec.create().name("test")), null);
    }

    @Test
    public void abortedCommandsAreReportedWithoutStacktrace() {
        RecordingConsole console = new RecordingConsole();
        int exitCode = handle(console, new RuntimeException(new ConsoleException.Interrupted()));

        assertEquals(ConsoleException.Interrupted.EXIT_CODE, exitCode);
        assertEquals(List.of("Aborted."), console.lines());
    }

    @Test
    public void abortReasonIsReported() {
        RecordingConsole console = new RecordingConsole();
        handle(console, new ConsoleException.Interrupted("STD_IN closed"));

        assertEquals(List.of("Aborted: STD_IN closed"), console.lines());
    }

    @Test
    public void otherExceptionsAreNotReportedAsAborted() {
        RecordingConsole console = new RecordingConsole();
        int exitCode = handle(console, new IllegalStateException("broken"));

        assertNotEquals(ConsoleException.Interrupted.EXIT_CODE, exitCode);
        assertEquals(List.of(), console.lines());
    }

}
