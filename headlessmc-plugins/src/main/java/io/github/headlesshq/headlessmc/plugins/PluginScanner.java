package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Finds the plugin jars in a directory and reads their {@link PluginManifest}s.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PluginScanner {
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    private final HashService hashService;
    private final JsonService jsonService;

    /**
     * Lists the jars in the given directory, sorted by file name.
     *
     * @param directory the plugins directory, does not need to exist.
     * @return a {@link PluginFile} for every jar in the directory.
     */
    public List<PluginFile> scan(Path directory) {
        List<Path> jars = list(directory);
        List<PluginFile> result = new ArrayList<>(jars.size());
        for (Path jar : jars) {
            result.add(read(jar));
        }

        return result;
    }

    /**
     * Lists the jars in the given directory, sorted by file name, without reading them.
     *
     * @param directory the plugins directory, does not need to exist.
     * @return the plugin jars in the directory.
     */
    public List<Path> list(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list plugins in " + directory, e);
        }
    }

    /**
     * Hashes the given jar and reads its manifest.
     *
     * @param jar the plugin jar.
     * @return the plugin file, with {@link PluginFile#error()} set if the manifest could not be read or is invalid.
     */
    public PluginFile read(Path jar) {
        String sha256 = hashService.hashFiles(List.of(jar), HashService.SHA256).hash();
        try (ZipFile zipFile = new ZipFile(jar.toFile())) {
            ZipEntry entry = zipFile.getEntry(PluginManifest.PATH);
            if (entry == null) {
                return new PluginFile(jar, sha256, null, "not a HeadlessMc plugin, " + PluginManifest.PATH + " is missing");
            }

            PluginManifest manifest;
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                manifest = jsonService.parse(inputStream, PluginManifest.class);
            }

            String error = validate(manifest);
            return new PluginFile(jar, sha256, manifest, error);
        } catch (IOException | JsonParseException e) {
            log.debug("Failed to read plugin {}", jar, e);
            return new PluginFile(jar, sha256, null, "failed to read " + PluginManifest.PATH + ": " + e.getMessage());
        }
    }

    private static @Nullable String validate(PluginManifest manifest) {
        if (manifest.schemaVersion() != PluginManifest.SCHEMA_VERSION) {
            return "unsupported schemaVersion " + manifest.schemaVersion() + ", expected " + PluginManifest.SCHEMA_VERSION;
        }

        if (manifest.id() == null || !ID.matcher(manifest.id()).matches()) {
            return "invalid id '" + manifest.id() + "', use lowercase letters, digits, - and _";
        }

        if (manifest.version() == null || manifest.headlessmc() == null) {
            return "the manifest needs a version and a headlessmc version range";
        }

        for (PluginManifest.Library library : manifest.libraryList()) {
            if (library.group() == null || library.artifact() == null || library.version() == null
                || library.path() == null || library.sha256() == null) {
                return "library " + library + " needs a group, artifact, version, path and sha256";
            }
        }

        for (PluginManifest.Dependency dependency : manifest.dependencyList()) {
            if (dependency.id() == null) {
                return "a dependency needs an id";
            }
        }

        return null;
    }

}
