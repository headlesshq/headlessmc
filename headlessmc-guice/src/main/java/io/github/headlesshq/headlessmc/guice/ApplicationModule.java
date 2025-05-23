package io.github.headlesshq.headlessmc.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.ApplicationImpl;
import io.github.headlesshq.headlessmc.api.command.*;
import io.github.headlesshq.headlessmc.api.command.picocli.CommandLineProvider;
import io.github.headlesshq.headlessmc.api.command.picocli.RootCommand;
import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.logging.Out;
import io.github.headlesshq.headlessmc.api.logging.StdIO;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import io.github.headlesshq.headlessmc.jline.JLineCommandLineReaderProvider;
import jakarta.inject.Singleton;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ApplicationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Injector.class).to(GuiceInjector.class).in(Singleton.class);
        bind(Application.class).to(ApplicationImpl.class).in(Singleton.class);
        bind(PicocliCommandContext.class).to(PicocliCommandContextImpl.class).in(Singleton.class);
        bind(CommandLineManager.class).to(CommandLineManagerImpl.class).in(Singleton.class);
        bind(Out.class).to(StdIO.class).in(Singleton.class);
        bind(CommandLine.class).toProvider(CommandLineProvider.class).in(Singleton.class);
        bind(CommandLineReader.class)
                .annotatedWith(Names.named("fallback"))
                .toProvider(DefaultCommandLineReaderProvider.class)
                .in(Singleton.class);
        bind(CommandLineReader.class)
                .toProvider(JLineCommandLineReaderProvider.class)
                .in(Singleton.class);
    }

    @Provides
    @Singleton
    public SettingGroup rootSettingGroup() {
        return SettingGroup.create("hmc", "HeadlessMc Settings.");
    }

    @Provides
    @Singleton
    public Config config() throws IOException {
        Path tempDir = Files.createTempDirectory("headlessmc");
        Path tempProperties = Files.createTempFile(tempDir, "headlessmc", ".properties");
        return Config.load(tempDir, tempProperties);
    }

    @Provides
    @Singleton
    @RootCommand
    public Object rootCommand() {
        return HeadlessMcCommand.class;
    }

}
