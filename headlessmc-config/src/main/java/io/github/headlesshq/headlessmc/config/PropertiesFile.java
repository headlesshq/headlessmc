package io.github.headlesshq.headlessmc.config;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Reads and edits a {@code .properties} file.
 * Editing uses Apache Commons Configuration, which, unlike {@link Properties#store},
 * keeps comments, blank lines and the order of existing entries intact.
 */
final class PropertiesFile {
    private PropertiesFile() {
        throw new AssertionError();
    }

    /**
     * Reads a properties file as UTF-8 with {@link Properties}, the reference implementation,
     * which SmallRye uses for its properties files too.
     *
     * @param file the file to read.
     * @return the properties in the file, or an empty map if the file does not exist.
     * @throws IOException if reading fails.
     * @throws IllegalArgumentException if the file contains a malformed unicode escape.
     */
    static Map<String, String> load(Path file) throws IOException {
        Map<String, String> result = new HashMap<>();
        if (!Files.exists(file)) {
            return result;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        for (String name : properties.stringPropertyNames()) {
            result.put(name, properties.getProperty(name));
        }

        return result;
    }

    /**
     * Sets a property in the given file, keeping the rest of the file as it is.
     * The file and its parent directories are created if they do not exist.
     *
     * @param file  the file to edit.
     * @param name  the name of the property.
     * @param value the new value of the property.
     * @throws ConfigurationException if reading or writing fails.
     */
    static void set(Path file, String name, String value) throws ConfigurationException {
        PropertiesConfiguration configuration = Files.exists(file) ? read(file) : newConfiguration();
        PropertiesConfigurationLayout layout = configuration.getLayout();
        if (configuration.isEmpty()) {
            // a file with only comments has them as footer, which would put new properties above them
            layout.setHeaderComment(layout.getFooterComment());
            layout.setFooterComment(null);
        }

        if (!configuration.containsKey(name)) {
            layout.setSeparator(name, "="); // the default is " = "
        }

        configuration.setProperty(name, value);
        write(configuration, file);
    }

    private static PropertiesConfiguration newConfiguration() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        // escapeUnicode = false, we write UTF-8
        configuration.setIOFactory(new PropertiesConfiguration.JupIOFactory(false));
        configuration.setIncludesAllowed(false);
        return configuration;
    }

    private static PropertiesConfiguration read(Path file) throws ConfigurationException {
        PropertiesConfiguration configuration = newConfiguration();
        fileHandler(configuration).load(file.toFile());
        return configuration;
    }

    private static void write(PropertiesConfiguration configuration, Path file) throws ConfigurationException {
        Path temp = file.resolveSibling(file.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // write to a temporary file first, so that the config file is never left half-written
            fileHandler(configuration).save(temp.toFile());
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Failed to write " + file, e);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private static FileHandler fileHandler(PropertiesConfiguration configuration) {
        FileHandler handler = new FileHandler(configuration);
        handler.setEncoding(StandardCharsets.UTF_8.name());
        return handler;
    }

}
