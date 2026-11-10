package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.WorldService;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Helpers for resolving the {@link Platform}, {@link ModType} and target directories
 * of a {@link Profile} that mods should be installed to or read from.
 */
@UtilityClass
final class Mods {
    /**
     * Resolves the {@link Platform} of the given profile.
     *
     * @param platformService the service to look the platform up on.
     * @param profile         the profile to resolve the platform of.
     * @return the platform of the profile.
     * @throws IllegalArgumentException if the platform cannot be found.
     */
    static Platform getPlatform(PlatformService platformService, Profile profile) {
        String platform = profile.version().platform();
        return platformService.getPlatform(platform).orElseThrow(() -> new IllegalArgumentException(
            "Failed to find platform " + platform + " for profile " + profile.name()
        ));
    }

    /**
     * Resolves the {@link ModType} for the given name, or falls back to a default if no name is given.
     * The default is the first mod type of the profile's platform,
     * {@link ModType#RESOURCE_PACK} for platforms without mod support
     * and {@link ModType#MOD} if no profile is given.
     *
     * @param platformService the service to look the platform up on.
     * @param modTypes        all known mod types.
     * @param profile         the profile the mod type is resolved for.
     * @param name            the name of the mod type, or {@code null} to use the default.
     * @return the resolved mod type.
     * @throws IllegalArgumentException if the mod type cannot be resolved.
     */
    static ModType resolveType(
        PlatformService platformService,
        Set<ModType> modTypes,
        @Nullable Profile profile,
        @Nullable String name
    ) {
        if (name != null) {
            return modTypes.stream()
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown mod type '" + name + "'."));
        }

        if (profile == null) {
            return ModType.MOD;
        }

        Optional<ModSupport> modSupport = getPlatform(platformService, profile).getModSupport();
        if (modSupport.isEmpty()) {
            return ModType.RESOURCE_PACK;
        }

        return modSupport.get().modTypes().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Platform " + profile.version().platform() + " does not declare a default mod type, "
                    + "please specify one with --type."
            ));
    }

    /**
     * Resolves the directory the given mod type is installed to for the given profile.
     *
     * <p>For most mod types this is derived from the game directory, datapacks however
     * are installed into the {@code datapacks} folder of a world.
     *
     * @param worldService the service to resolve the world with.
     * @param profile      the profile to resolve the target directory for.
     * @param types        the mod types to resolve the directory of.
     * @param world        the name of the world (only used for datapacks), or {@code null}.
     * @return the directory the mod type is installed to.
     */
    static Path targetDir(WorldService worldService, Profile profile, Set<ModType> types, @Nullable String world) {
        if (types.stream().anyMatch(Mods::isDatapack)) {
            if (types.size() > 1) {
                throw new IllegalArgumentException("Cannot use multiple mod types with datapac");
            }

            return types.stream().findFirst().orElseThrow().directory().getDir(worldService.resolveWorld(profile, world));
        }

        return profile.path();
    }

    /**
     * @param type the mod type to check.
     * @return {@code true} if the given mod type is a datapack and needs a world.
     */
    static boolean isDatapack(ModType type) {
        return ModType.DATA_PACK.name().equals(type.name());
    }

}
