package io.github.headlesshq.headlessmc.mods.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

// TODO:
//  - FTB
//  - FTB Legacy
//  - ATLauncher
//  - Technic

/**
 * Represents a platform that you can download mods from.
 * Think {@code Modrinth} or {@code Curseforge}.
 */
public interface ModDistributionPlatform {
    /**
     * Name of the platform that combines the {@link #DEFAULT_PLATFORMS}.
     */
    String DEFAULT = "default";

    /**
     * Name of the platform that combines all {@link #isEnabled() enabled} platforms.
     */
    String ALL = "all";

    /**
     * Names of the platforms used by {@link #DEFAULT}, in the order in which they are tried when downloading.
     */
    List<String> DEFAULT_PLATFORMS = List.of("github", "modrinth");

    /**
     * Searches the platform for mods that match the given query and are of the given {@link ModType}.
     *
     * @param query the query to search for.
     * @param type  the type of mods to search for.
     * @return a list of {@link RemoteMod} that are related to the search query.
     * @throws HeadlessMcException if something goes wrong.
     */
    List<RemoteMod> search(String query, ModType type) throws HeadlessMcException;

    /**
     * Searches the platform for mods that match the given query,
     * match the given version and are of the given {@link ModType}.
     *
     * @param query   the query to search for.
     * @param version the mc version the mods should be for.
     * @param type    the type of mods to search for.
     * @return a list of {@link RemoteMod} that are related to the search query.
     * @throws HeadlessMcException if something goes wrong.
     */
    List<RemoteMod> search(String query, VersionArg version, ModType type) throws HeadlessMcException;

    /**
     * Downloads a Mod to the specified location.
     *
     * @param dir      the directory to download the mod to.
     * @param worldDir the world to download datapacks to.
     * @param id       the id/slug of the mod ({@link RemoteMod#id()}).
     * @param version  the game version the mod should be for.
     * @param type     the {@link ModType} the mod should be of.
     * @return the file the mod has been downloaded to.
     * @throws HeadlessMcException if something goes wrong.
     */
    Path download(
        Path dir,
        @Nullable Path worldDir,
        String id,
        VersionArg version,
        ModType type
    ) throws HeadlessMcException;

    // Optional<RemoteMod> findByHash(Map<String, String> hashes, long size);

    /**
     * Platforms that need configuration, e.g. an api key, can be disabled.
     * Disabled platforms are skipped by {@link #DEFAULT} and {@link #ALL}.
     *
     * @return {@code true} if this platform can be used.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * @return the name of this {@link ModDistributionPlatform}.
     */
    String getName();

}
