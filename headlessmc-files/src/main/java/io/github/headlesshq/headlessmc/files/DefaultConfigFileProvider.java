package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.ConfigFileProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Places the config.properties file in the {@link AppFiles#getConfigDir()}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DefaultConfigFileProvider implements ConfigFileProvider {
    private final AppFiles appFiles;

    @Override
    public Path getConfigFile() {
        return appFiles.getConfigDir().resolve(CONFIG_FILE_NAME);
    }

}
