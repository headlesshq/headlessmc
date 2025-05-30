package io.github.headlesshq.headlessmc.api.settings;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// TODO: away with this?!
// TODO: global config
// TODO: chose config file based on SystemProperty
//  list of config files, that override each other in that order!
@ApplicationScoped
public class ConfigProducer {
    @Produces
    @Default
    @ApplicationScoped
    public Config config() throws IOException {
        Path tempDir = Files.createTempDirectory("HeadlessMc");
        Path tempProperties = Files.createTempFile(tempDir, "HeadlessMc", ".properties");
        return Config.load(tempDir, tempProperties);
    }

}
