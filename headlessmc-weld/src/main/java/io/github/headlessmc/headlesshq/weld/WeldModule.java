package io.github.headlessmc.headlesshq.weld;

import io.github.headlesshq.headlessmc.api.command.picocli.RootCommand;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class WeldModule {
    @Default
    @Produces
    @ApplicationScoped
    public SettingGroup rootSettingGroup() {
        return SettingGroup.create("hmc", "HeadlessMc Settings.");
    }

    @Produces
    @Default
    @ApplicationScoped
    public Config config() throws IOException {
        Path tempDir = Files.createTempDirectory("headlessmc");
        Path tempProperties = Files.createTempFile(tempDir, "headlessmc", ".properties");
        return Config.load(tempDir, tempProperties);
    }

    @Produces
    @RootCommand
    @ApplicationScoped
    public Object rootCommand() {
        return HeadlessMcCommand.class;
    }

}
