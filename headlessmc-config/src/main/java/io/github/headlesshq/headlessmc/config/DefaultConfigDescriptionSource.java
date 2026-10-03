package io.github.headlesshq.headlessmc.config;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@ApplicationScoped
@RegisterResources(globs = DefaultConfigDescriptionSource.RESOURCE_NAME)
public class DefaultConfigDescriptionSource implements ConfigDescriptionSource {
    static final String RESOURCE_NAME = "config-descriptions.properties";
    private final Properties properties;

    public DefaultConfigDescriptionSource() {
        // TODO: what if multiple resources on classpath?
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (inputStream == null) {
                throw new HeadlessMcIOException("Failed to find resource " + RESOURCE_NAME);
            }

            this.properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to read resource " + RESOURCE_NAME, e);
        }
    }

    @Override
    public Optional<String> getDescription(String name) {
        return Optional.ofNullable((String) properties.get(name));
    }

}
