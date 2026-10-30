package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Access to a checkout of <a href="https://github.com/headlesshq/mc-versions">mc-versions</a>,
 * which contains the version.jsons of thousands of vanilla, Forge, NeoForge and Fabric versions.
 * It is searched next to the HeadlessMc project, in any parent directory of the working directory,
 * or at the path given by the system property or environment variable {@value #PROPERTY}.
 */
public final class McVersions {
    public static final String PROPERTY = "MC_VERSIONS";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static @Nullable McVersions instance;

    private final Path versionsDir;
    private @Nullable List<McVersion> versions;

    private McVersions(Path versionsDir) {
        this.versionsDir = versionsDir;
    }

    /**
     * @return the mc-versions checkout, or none if it could not be found.
     */
    public static synchronized Optional<McVersions> find() {
        if (instance != null) {
            return Optional.of(instance);
        }

        String configured = System.getProperty(PROPERTY, System.getenv(PROPERTY));
        if (configured != null) {
            Path dir = Paths.get(configured);
            return Optional.of(instance = new McVersions(Files.isDirectory(dir.resolve("versions"))
                ? dir.resolve("versions")
                : dir));
        }

        for (Path dir = Paths.get("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path versionsDir = dir.resolve("mc-versions").resolve("versions");
            if (Files.isDirectory(versionsDir)) {
                return Optional.of(instance = new McVersions(versionsDir));
            }
        }

        return Optional.empty();
    }

    /**
     * @return the directory containing the version directories and the version manifest.
     */
    public Path versionsDir() {
        return versionsDir;
    }

    /**
     * @return all version.jsons in mc-versions, sorted by id.
     */
    public synchronized List<McVersion> versions() {
        if (versions == null) {
            try (Stream<Path> directories = Files.list(versionsDir)) {
                versions = directories
                    .filter(Files::isDirectory)
                    .map(dir -> dir.resolve(dir.getFileName() + ".json"))
                    .filter(Files::exists)
                    .parallel()
                    .map(McVersions::read)
                    .sorted(Comparator.comparing(McVersion::id))
                    .toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return versions;
    }

    /**
     * @return a {@link VanillaVersionService} with all versions of the version manifest in mc-versions.
     */
    public FakeVanillaVersionService vanillaVersionService() {
        JsonNode manifest = readTree(versionsDir.resolve("version_manifest_v2.json"));
        List<String> names = new ArrayList<>();
        for (JsonNode version : manifest.get("versions")) {
            names.add(version.get("id").asString());
        }

        return new FakeVanillaVersionService(names.toArray(String[]::new));
    }

    /**
     * @param fileName a json file in the versions directory of mc-versions, e.g. {@code forge_legacy_versions.json}.
     * @return the content of the file.
     */
    public JsonNode readJson(String fileName) {
        return readTree(versionsDir.resolve(fileName));
    }

    private static McVersion read(Path json) {
        JsonNode node = readTree(json);
        List<String> libraries = new ArrayList<>();
        for (JsonNode library : node.path("libraries")) {
            libraries.add(library.get("name").asString());
        }

        List<String> gameArguments = new ArrayList<>();
        for (JsonNode argument : node.path("arguments").path("game")) {
            if (argument.isString()) {
                gameArguments.add(argument.asString());
            }
        }

        JsonNode inheritsFrom = node.get("inheritsFrom");
        return new McVersion(
            node.get("id").asString(),
            inheritsFrom == null || inheritsFrom.isNull() ? null : inheritsFrom.asString(),
            libraries,
            gameArguments
        );
    }

    private static JsonNode readTree(Path json) {
        try {
            return MAPPER.readTree(Files.readString(json));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The parts of a version.json from mc-versions that tell us what the version is.
     *
     * @param id            the id of the version.
     * @param inheritsFrom  the parent of the version.
     * @param libraries     the names of all libraries.
     * @param gameArguments the game arguments that are plain strings.
     */
    public record McVersion(
        String id,
        @Nullable String inheritsFrom,
        List<String> libraries,
        List<String> gameArguments
    ) {
        /**
         * @param name the name of an argument, e.g. {@code --fml.forgeVersion}.
         * @return the value following the argument.
         */
        public Optional<String> gameArgument(String name) {
            int index = gameArguments.indexOf(name);
            return index < 0 || index + 1 >= gameArguments.size()
                ? Optional.empty()
                : Optional.of(gameArguments.get(index + 1));
        }

        /**
         * @param prefix the prefix of a library name, e.g. {@code net.fabricmc:fabric-loader:}.
         * @return the first library name with that prefix.
         */
        public Optional<String> library(String prefix) {
            return libraries.stream().filter(library -> library.startsWith(prefix)).findFirst();
        }

        public Version toVersion() {
            return new FakeVersion(id).withInheritsFrom(inheritsFrom);
        }

        @Override
        public String toString() {
            return id;
        }
    }

}
