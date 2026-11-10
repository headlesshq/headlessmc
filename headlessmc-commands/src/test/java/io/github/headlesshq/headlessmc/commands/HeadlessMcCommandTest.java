package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.FinalCommand;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.cache.ConsoleCachingService;
import io.github.headlesshq.headlessmc.updater.UpdateNotifier;
import io.github.headlesshq.headlessmc.updater.Updater;
import io.github.headlesshq.headlessmc.updater.UpdaterConfig;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeadlessMcCommandTest {
    private final List<String> executed = new ArrayList<>();
    private final RecordingConsole console = new RecordingConsole();

    @CommandLine.Command(name = "root")
    static class Root implements Runnable {
        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "launch")
    class Launch implements Runnable, FinalCommand {
        @CommandLine.Option(names = "--dry-run")
        boolean dryRun;

        @CommandLine.Parameters(defaultValue = "")
        String version;

        @Override
        public void run() {
            executed.add(version + (dryRun ? " (dry run)" : ""));
        }

        @Override
        public boolean isFinal() {
            return !dryRun;
        }
    }

    @SuppressWarnings("unchecked")
    private HeadlessMcCommand command() {
        UpdaterConfig config = Mockito.mock(UpdaterConfig.class);
        Holder<UpdaterConfig> holder = () -> config;
        UpdateNotifier notifier = new UpdateNotifier(Mockito.mock(Instance.class), holder, console);
        return new HeadlessMcCommand(
            () -> new CommandLine(new Root()).addSubcommand(new Launch()),
            new ConsoleCachingService(),
            line -> line.isBlank() ? new String[0] : line.trim().split(" "),
            console,
            notifier
        );
    }

    @Test
    void shellKeepsReadingAfterADryRunAndExitsAfterAFinalCommand() {
        console.addInput("launch --dry-run 1.21.5")
            .addInput("launch 1.21.5")
            .addInput("launch 1.20.1");

        assertEquals(0, command().call());
        assertEquals(List.of("1.21.5 (dry run)", "1.21.5"), executed);
    }

}
