package io.github.headlesshq.headlessmc.config;

import java.nio.file.Path;

/**
 * Provides the location of the config.properties file
 * that the {@link ConfigService} reads properties from
 * and persists properties to.
 * If no bean of this type exists, the {@link ConfigService}
 * does not use a config file.
 */
public interface ConfigFileProvider {
    /**
     * Name of the config file.
     */
    String CONFIG_FILE_NAME = "config.properties";

    /**
     * @return the location of the config file, which might not exist yet.
     */
    Path getConfigFile();

}
