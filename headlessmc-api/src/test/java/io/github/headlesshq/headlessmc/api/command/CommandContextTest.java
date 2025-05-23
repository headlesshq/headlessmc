package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.TestApplication;
import io.github.headlesshq.headlessmc.api.command.picocli.CommandLineProvider;
import io.github.headlesshq.headlessmc.api.logging.PrintWriterPrintStream;
import io.github.headlesshq.headlessmc.api.logging.ReadableOutputStream;
import lombok.Cleanup;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandContextTest {
    @Test
    public void test() throws IOException {
        Application app = TestApplication.create();
        @Cleanup
        ReadableOutputStream os = new ReadableOutputStream();
        @Cleanup
        ReadableOutputStream err = new ReadableOutputStream();
        app.getCommandLine().getStdIO().setOut(() -> new PrintWriterPrintStream(os, true));
        app.getCommandLine().getStdIO().setErr(() -> new PrintWriterPrintStream(err, true));

        CommandLine commandLine = new CommandLineProvider(app.getCommandLine().getStdIO(), app.getInjector(), RootTestCommand.class).get();
        PicocliCommandContext context = new PicocliCommandContextImpl(commandLine);
        context.execute("-h");

        String output = read(os.getInputStream());
        String expected = "Usage: root [-hV] [COMMAND]\n" +
                "Test Command2\n" +
                "  -h, --help      Show this help message and exit.\n" +
                "  -V, --version   Print version information and exit.\n" +
                "Commands:\n" +
                "  test1  Test Command2";
        assertEquals(expected, output);

        String errorOutput = read(err.getInputStream());
        assertEquals("", errorOutput);

        context.execute("test1");
        assertEquals("hello", context.getPicocli().getSubcommands().get("test1").getExecutionResult());

        String out = read(os.getInputStream());
        System.out.println(out);

        context.execute("test1 --test world");
        assertEquals("world", context.getPicocli().getSubcommands().get("test1").getExecutionResult());


        //context.execute("test1");

        //assertEquals("hello", context.getPicocli().getExecutionResult());

        //context.execute("test2");
        //errorOutput = read(err.getInputStream());
        //System.out.println(errorOutput);
    }

    private String read(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            return String.join("\n", lines);
        }
    }

    @CommandLine.Command(
        name = "root",
        version = HeadlessMc.NAME_VERSION,
        mixinStandardHelpOptions = true,
        description = "Test Command2",
        subcommands = {TestCommand1.class}
    )
    private static class RootTestCommand {

    }

    @CommandLine.Command(
        name = "test1",
        version = HeadlessMc.NAME_VERSION,
        mixinStandardHelpOptions = true,
        description = "Test Command2"
    )
    private static class TestCommand1 implements Callable<String> {
        @CommandLine.Option(names = "--test")
        private String option = "hello";

        @Override
        public String call() {
            return option;
        }
    }

    @CommandLine.Command(
        name = "test2",
        version = HeadlessMc.NAME_VERSION,
        mixinStandardHelpOptions = true,
        description = "Test Command2"
    )
    private static class TestCommand2 implements Callable<String> {
        @CommandLine.Option(names = "-option")
        private String option = "world";

        @Override
        public String call() {
            return option;
        }
    }

}
