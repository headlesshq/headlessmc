package io.github.headlesshq.headlessmc.mods.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

// TODO:
//  - CurseForge
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
     * By default, we use {@code Modrinth}, there are no implementations for other platforms yet
     */
    String DEFAULT = "modrinth";

    // TODO: this should just take one ModType!
    /**
     * Searches the platform for mods that match the given query and are of the given {@link ModType}s.
     *
     * @param query the query to search for.
     * @param types the types of mods to search for.
     * @return a list of {@link RemoteMod} that are related to the search query.
     * @throws HeadlessMcException if something goes wrong.
     */
    List<RemoteMod> search(String query, Set<ModType> types) throws HeadlessMcException;

    // TODO: this should just take one ModType!
    /**
     * Searches the platform for mods that match the given query,
     * match the given version and are of the given {@link ModType}s.
     *
     * @param query   the query to search for.
     * @param version the mc version the mods should be for.
     * @param types   the types of mods to search for.
     * @return a list of {@link RemoteMod} that are related to the search query.
     * @throws HeadlessMcException if something goes wrong.
     */
    List<RemoteMod> search(String query, VersionArg version, Set<ModType> types) throws HeadlessMcException;

    /**
     * Downloads a Mod to the specified location.
     *
     * @param dir      the directory to download the mod to.
     * @param worldDir the world to download datapacks to.
     * @param id       the id/slug of the mod ({@link RemoteMod#id()}).
     * @param version  the game version the mod should be for.
     * @param types    the {@link ModType}s the mod should be of.
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
     * @return the name of this {@link ModDistributionPlatform}.
     */
    String getName();

}
