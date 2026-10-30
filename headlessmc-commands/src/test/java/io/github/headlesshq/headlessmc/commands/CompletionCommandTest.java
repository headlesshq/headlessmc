package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompletionCommandTest {
    private final RecordingConsole console = new RecordingConsole();

    @TempDir
    Path dir;

    private CommandLine commandLine(StringWriter out) {
        CommandLine commandLine = new CommandLine(
            new CompletionCommand(new DefaultFileService(new DefaultFileSystemProvider()), console)
        );
        commandLine.setOut(new PrintWriter(out));
        return commandLine;
    }

    @ParameterizedTest
    @EnumSource(CompletionCommand.Shell.class)
    void printsTheScriptForEveryShell(CompletionCommand.Shell shell) {
        StringWriter out = new StringWriter();

        assertEquals(0, commandLine(out).execute(shell.name()));
        assertTrue(out.toString().contains(ShellCompletions.COMMAND), () -> "was: " + out);
    }

    @Test
    void writesTheScriptToAFileAndOverwritesIt() throws IOException {
        StringWriter printed = new StringWriter();
        commandLine(printed).execute("zsh");

        Path file = dir.resolve("completions").resolve("_headlessmc");
        StringWriter out = new StringWriter();
        assertEquals(0, commandLine(out).execute("zsh", "--output", file.toString()));
        assertEquals(0, commandLine(out).execute("zsh", "-o", file.toString()));

        assertEquals(printed.toString(), Files.readString(file));
        assertEquals("", out.toString());
        assertTrue(console.output().contains(file.toAbsolutePath().toString()), console::output);
    }

}
