package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Locates and lists the worlds of a {@link Profile}.
 *
 * <p>For {@code CLIENT} profiles worlds live inside the {@code saves} directory of the
 * game directory. For {@code SERVER} profiles each world is a directory in the profile
 * root, identified by containing a {@code datapacks} folder or a {@code level.dat}.
 * The default world of a server is the {@code level-name} of its {@code server.properties},
 * or {@code world} if none is configured.
 */
@Slf4j
@ApplicationScoped
public class WorldService {
    public static final String SAVES = "saves";
    public static final String DATAPACKS = "datapacks";
    public static final String LEVEL_DAT = "level.dat";
    public static final String SERVER_PROPERTIES = "server.properties";
    public static final String LEVEL_NAME = "level-name";
    public static final String DEFAULT_SERVER_WORLD = "world";

    /**
     * Lists the worlds of the given profile.
     *
     * @param profile the profile to list the worlds of.
     * @return the world directories of the profile, sorted by name.
     */
    public List<Path> listWorlds(Profile profile) {
        if (profile.side().isClient()) {
            return listDirectories(getSavesDir(profile));
        }

        return listDirectories(profile.path()).stream()
            .filter(dir -> Files.isDirectory(dir.resolve(DATAPACKS)) || Files.isRegularFile(dir.resolve(LEVEL_DAT)))
            .toList();
    }

    /**
     * Resolves the world directory of the given profile.
     *
     * <p>If no world is specified, the single world of a {@code CLIENT} profile or the
     * default world of a {@code SERVER} profile is used.
     *
     * @param profile the profile to resolve the world of.
     * @param world   the name of the world, or {@code null} to use the default.
     * @return the resolved world directory.
     * @throws IllegalArgumentException if the world cannot be resolved.
     */
    public Path resolveWorld(Profile profile, @Nullable String world) {
        if (profile.side().isClient()) {
            Path saves = getSavesDir(profile);
            if (world != null) {
                return requireDirectory(saves.resolve(world), profile, world);
            }

            List<Path> worlds = listDirectories(saves);
            if (worlds.size() == 1) {
                return worlds.getFirst();
            }

            throw new IllegalArgumentException(
                "Please specify a world with --world, found " + worlds.size() + " worlds in " + saves
            );
        }

        if (world != null) {
            return requireDirectory(profile.path().resolve(world), profile, world);
        }

        String defaultWorldName = getDefaultServerWorldName(profile);
        Path defaultWorld = profile.path().resolve(defaultWorldName);
        if (Files.isDirectory(defaultWorld)) {
            return defaultWorld;
        }

        throw new IllegalArgumentException(
            "Please specify a world with --world, no default '" + defaultWorldName
                + "' world found in " + profile.path()
        );
    }

    /**
     * @param profile the profile to get the saves directory for.
     * @return the directory containing the worlds of a {@code CLIENT} profile.
     */
    public Path getSavesDir(Profile profile) {
        return profile.path().resolve(SAVES);
    }

    /**
     * Reads the {@code level-name} of the {@code server.properties} of the given server profile.
     *
     * @param profile the server profile.
     * @return the name of the default world of the server.
     */
    public String getDefaultServerWorldName(Profile profile) {
        Path serverProperties = profile.path().resolve(SERVER_PROPERTIES);
        if (!Files.isRegularFile(serverProperties)) {
            return DEFAULT_SERVER_WORLD;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(serverProperties, StandardCharsets.ISO_8859_1)) {
            properties.load(reader);
        } catch (IOException e) {
            log.warn("Failed to read {}", serverProperties, e);
            return DEFAULT_SERVER_WORLD;
        }

        String levelName = properties.getProperty(LEVEL_NAME);
        return levelName == null || levelName.isBlank() ? DEFAULT_SERVER_WORLD : levelName.trim();
    }

    private Path requireDirectory(Path dir, Profile profile, String world) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException(
                "Failed to find world '" + world + "' for profile " + profile.name() + " at " + dir
            );
        }

        return dir;
    }

    private List<Path> listDirectories(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to list directory " + dir, e);
        }
    }

}
