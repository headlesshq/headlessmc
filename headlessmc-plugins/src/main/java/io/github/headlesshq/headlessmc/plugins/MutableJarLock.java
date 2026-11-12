package io.github.headlesshq.headlessmc.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Describes the {@code headlessmc-mutable.zip} that has been built together with this version of HeadlessMc.
 * The build generates {@value #RESOURCE} when it is given {@code -PmutableJarLock=<file>}.
 *
 * @param version the HeadlessMc version.
 * @param sha256  the SHA-256 hash of the zip.
 * @param size    the size of the zip in bytes.
 * @param url     where to download the zip from.
 */
public record MutableJarLock(String version, String sha256, long size, String url) {
    /**
     * The resource containing the lock.
     */
    public static final String RESOURCE = "META-INF/headlessmc/mutable-jar.properties";

    /**
     * @return the lock bundled with this version of HeadlessMc, or empty if it has been built without one.
     */
    public static Optional<MutableJarLock> load() {
        try (InputStream inputStream = MutableJarLock.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (inputStream == null) {
                return Optional.empty();
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            return Optional.of(new MutableJarLock(
                properties.getProperty("version"),
                properties.getProperty("sha256"),
                Long.parseLong(properties.getProperty("size")),
                properties.getProperty("url")
            ));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + RESOURCE, e);
        }
    }

}
