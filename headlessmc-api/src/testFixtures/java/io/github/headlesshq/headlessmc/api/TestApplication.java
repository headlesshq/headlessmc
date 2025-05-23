package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.command.CommandLineManager;
import io.github.headlesshq.headlessmc.api.command.CommandLineManagerImpl;
import io.github.headlesshq.headlessmc.api.command.PicocliCommandContextImpl;
import io.github.headlesshq.headlessmc.api.command.picocli.CommandLineProvider;
import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.api.logging.StdIO;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import picocli.CommandLine;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class TestApplication extends ApplicationImpl {
    private TestApplication(SettingGroup settings,
                           Injector injector,
                           ExitManager exitManager,
                           CommandLineManager commandLine,
                           Config config) {
        super(settings, injector, exitManager, commandLine, config);
    }

    public static TestApplication create() {
        try {
            Path tempDir = Files.createTempDirectory("headlessmc");
            Path tempProperties = Files.createTempFile(tempDir, "headlessmc", ".properties");
            Config config = Config.load(tempDir, tempProperties);
            ExitManager exitManager = new ExitManager();
            exitManager.setExitManager(i -> {});

            StdIO stdio = new StdIO();
            Injector injector = new TestInjector();
            CommandLineProvider commandLineProvider = new CommandLineProvider(stdio, injector, TestCommand.class);
            CommandLine commandLine = commandLineProvider.create();

            return new TestApplication(SettingGroup.create("hmc", "test"),
                    new TestInjector(),
                    new ExitManager(),
                    new CommandLineManagerImpl(
                            new PicocliCommandContextImpl(commandLine),
                            application -> {},
                            stdio
                    ),
                    config);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @CommandLine.Command(
        name = "test"
    )
    public static class TestCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            return 0;
        }
    }

}
