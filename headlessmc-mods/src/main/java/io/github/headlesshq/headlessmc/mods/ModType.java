package io.github.headlesshq.headlessmc.mods;

import java.util.Map;

/**
 * Represents a certain type of modification that can be made to the game.
 * This includes of course mods and plugins.
 * But also resource packs, shader packs, data packs and mod packs.
 *
 * @param name              the name of this ModType.
 * @param distributionNames a map of names for the API of ModDistributions to
 *                          download from.
 * @param directory         provides the directory relative to the
 *                          game directory mods of this type
 *                          should be installed to.
 *                          (exception: {@link #DATA_PACK})
 */
public record ModType(
    String name,
    Map<String, String> distributionNames,
    ModDirectory directory
) {
    /**
     * Entry for the default distribution name in {@link #distributionNames}.
     */
    public static final String DISTRIBUTION_DEFAULT = "default";

    /**
     * The default mod type, for platforms like Fabric, Forge or Neoforge.
     */
    public static final ModType MOD = new ModType(
        "mod",
        Map.of(DISTRIBUTION_DEFAULT, "mod"),
        ModDirectory.of("mods")
    );

    /**
     * Plugins for server platforms like Paper, Spigot, Bukkit, Purpur, SpongePowered etc.
     */
    public static final ModType PLUGIN = new ModType(
        "plugin",
        Map.of(DISTRIBUTION_DEFAULT, "plugin"),
        ModDirectory.of("plugins")
    );

    /**
     * Resource packs that e.g. bring textures or sounds to the game.
     */
    public static final ModType RESOURCE_PACK = new ModType(
        "resourcepack",
        Map.of(DISTRIBUTION_DEFAULT, "resourcepack"),
        ModDirectory.of("resourcepacks")
    );

    /**
     * Modpacks are a full installation with resourcepacks, mods, shaderpacks etc.
     * The {@link #directory} for this is the gameDir itself and the modifications
     * contained in a mod pack need to be extracted to their respective sub dirs.
     */
    public static final ModType MOD_PACK = new ModType(
        "modpack",
        Map.of(DISTRIBUTION_DEFAULT, "modpack"),
        gameDir -> gameDir
    );

    /**
     * Data packs, a way to e.g. mod vanilla.
     * Datapacks are an exception regarding their {@link #directory},
     * because they are installed in the world folders.
     */
    public static final ModType DATA_PACK = new ModType(
        "datapack",
        Map.of(DISTRIBUTION_DEFAULT, "datapack"), // TODO: is this implemented in modrinth?
        // that this is world dir is not so nice, and requires special handling,
        // but for now it's okay, I don't see new ModTypes coming anytime soon,
        // that would also need special handling so we can make this an exception
        worldDir -> worldDir.resolve("datapacks")
    );
    /**
     * Shader packs require other {@link #MOD}s to function.
     * The location of the {@link #directory} to place them in is
     * decided by those other mods, like Iris or Optifine.
     * Usually all of these use the {@code shaderpacks} folder.
     */
    public static final ModType SHADER = new ModType(
        "shader",
        Map.of(DISTRIBUTION_DEFAULT, "shader"),
        // TODO: find out where shaders are commonly installed to
        gameDir -> gameDir.resolve("shaderpacks")
    );

}
