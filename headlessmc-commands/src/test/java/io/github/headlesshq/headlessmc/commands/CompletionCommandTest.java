package io.github.headlesshq.headlessmc.commands;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompletionCommandTest {
    @ParameterizedTest
    @EnumSource(CompletionCommand.Shell.class)
    void printsTheScriptForEveryShell(CompletionCommand.Shell shell) {
        StringWriter out = new StringWriter();
        CommandLine commandLine = new CommandLine(new CompletionCommand());
        commandLine.setOut(new PrintWriter(out));

        assertEquals(0, commandLine.execute(shell.name()));
        assertTrue(out.toString().contains(ShellCompletions.COMMAND), () -> "was: " + out);
    }

}
