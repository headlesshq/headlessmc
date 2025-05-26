package io.github.headlesshq.headlessmc.api.settings;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class ConfigProducer {
    @Produces
    @Default
    @ApplicationScoped
    public Config config() throws IOException {
        Path tempDir = Files.createTempDirectory("headlessmc");
        Path tempProperties = Files.createTempFile(tempDir, "headlessmc", ".properties");
        return Config.load(tempDir, tempProperties);
    }

}
