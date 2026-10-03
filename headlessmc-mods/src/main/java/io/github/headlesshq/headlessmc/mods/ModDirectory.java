package io.github.headlesshq.headlessmc.mods;

import java.nio.file.Path;

/**
 * Every type of modification ({@link ModType})
 * is installed to a certain directory in the game directory.
 * E.g. mods from Fabric, Forge or NeoForge are installed to {@code mods},
 * plugins for Paper, Spigot, Bukkit etc. are installed to {@code plugins}.
 * {@link #getDir(Path)} describes that directory for a {@link ModType}
 * based on a base directory, which usually the game directory.
 * One exception are {@link ModType#DATA_PACK}s.
 *
 * @see ModType
 * @see ModType#directory()
 */
@FunctionalInterface
public interface ModDirectory {
    /**
     * The directory to install mods in relative to the
     * base directory, usually the game directory.
     *
     * @param base the base directory.
     * @return a path relative to the base directory.
     */
    Path getDir(Path base);

    /**
     * Creates a mod directory that resolves to
     * a directory of the given name in the base directory.
     *
     * @param name the name of the child directory.
     * @return a mod directory that resolves to a directory of the given name.
     */
    static ModDirectory of(String name) {
        return base -> base.resolve(name);
    }
    
}
