package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.settings.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class TestConfigProvider {
    @Produces
    @ApplicationScoped
    public Config getConfig() throws IOException {
        Path path = Files.createTempDirectory("test");
        Path configPath = path.resolve("config.properties");
        return Config.load(path, configPath);
    }

}
