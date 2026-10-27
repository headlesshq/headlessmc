package io.github.headlesshq.headlessmc.mods.curseforge;

import io.github.headlesshq.headlessmc.mods.ModType;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Ids the CurseForge API uses for games, classes and mod loaders.
 *
 * @see <a href=https://docs.curseforge.com/rest-api/#tocS_ModLoaderType>
 * https://docs.curseforge.com/rest-api/#tocS_ModLoaderType</a>
 */
@UtilityClass
class CurseForgeIds {
    public static final int MINECRAFT = 432;

    private static final Map<String, Integer> CLASSES = Map.of(
        ModType.MOD.name(), 6,
        ModType.PLUGIN.name(), 5,
        ModType.RESOURCE_PACK.name(), 12,
        ModType.MOD_PACK.name(), 4471,
        ModType.SHADER.name(), 6552,
        ModType.DATA_PACK.name(), 6945
    );

    private static final Map<String, Integer> MOD_LOADERS = Map.of(
        "forge", 1,
        "fabric", 4,
        "quilt", 5,
        "neoforge", 6
    );

    /**
     * @param type the type of mod.
     * @return the CurseForge class id for the given type, or {@code null} if CurseForge does not know the type.
     */
    public static @Nullable Integer classId(ModType type) {
        return CLASSES.get(type.name());
    }

    /**
     * @param platform the platform of a version, e.g. {@code fabric}.
     * @return the CurseForge mod loader type, or {@code null} if the platform is not a mod loader.
     */
    public static @Nullable Integer modLoaderType(String platform) {
        return MOD_LOADERS.get(platform.toLowerCase(Locale.ENGLISH));
    }

}
