package io.github.headlesshq.headlessmc.commands;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShellCompletionsTest {
    @CommandLine.Command(name = "root", subcommands = {Launch.class, Load.class})
    static class Root {
    }

    @SuppressWarnings("unused")
    @CommandLine.Command(name = "launch", description = {"Launches", "a @|bold version|@.%n"})
    static class Launch {
        @CommandLine.Option(names = "--jvm", description = "JVM\targs")
        String jvm;

        @CommandLine.Option(names = "--packwiz", description = "packwiz file")
        Path packwiz;

        @CommandLine.Parameters(index = "0", completionCandidates = Versions.class)
        String version;
    }

    @SuppressWarnings("unused")
    @CommandLine.Command(name = "load", description = "Loads a file.")
    static class Load {
        @CommandLine.Parameters(index = "0")
        Path file;
    }

    static class Versions extends java.util.ArrayList<String> {
        Versions() {
            super(List.of("1.21.4", "26.1"));
        }
    }

    private List<String> complete(String... words) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int exitCode = new ShellCompletions().complete(
            new CommandLine(Root.class),
            List.of(words),
            new PrintStream(bytes, true, StandardCharsets.UTF_8)
        );

        assertEquals(0, exitCode);
        return bytes.toString(StandardCharsets.UTF_8).lines().toList();
    }

    @Test
    void completesSubcommandsWithDescriptions() {
        List<String> lines = complete("");

        assertTrue(lines.contains("launch\tLaunches a version."), () -> "was: " + lines);
        assertTrue(lines.contains("load\tLoads a file."), () -> "was: " + lines);
        assertEquals(":" + ShellCompletions.DIRECTIVE_NO_FILE, lines.getLast());
    }

    @Test
    void completesWithNoArgumentsAtAll() {
        assertTrue(complete().contains("launch\tLaunches a version."));
    }

    @Test
    void completesPrefixesAndOptions() {
        assertEquals(List.of("launch\tLaunches a version.", ":4"), complete("lau"));
        assertTrue(complete("launch", "--j").contains("--jvm\tJVM args"));
    }

    @Test
    void completesPositionalCandidatesWithoutDescription() {
        List<String> lines = complete("launch", "");

        assertTrue(lines.contains("1.21.4\t"), () -> "was: " + lines);
        assertTrue(lines.contains("26.1\t"), () -> "was: " + lines);
    }

    @Test
    void fallsBackToFilesOnlyForPaths() {
        assertEquals(List.of(":0"), complete("launch", "--packwiz", ""));
        assertEquals(List.of(":0"), complete("load", ""));
        assertEquals(List.of(":4"), complete("launch", "--jvm", ""));
        assertEquals(List.of(":4"), complete("launch", "1.21.4", "x"));
    }

}
