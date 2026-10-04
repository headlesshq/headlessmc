package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.commands.util.ProfileVersionCompletions;
import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.ModTypeService;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatformService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:mod:add")
@CommandLine.Command(
    name = "add",
    aliases = {"download", "install"},
    mixinStandardHelpOptions = true,
    description = "Downloads and adds a mod to a profile."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AddCommand implements Runnable, VersionArgCommand, CachedConsole.Enabled {
    private final ModDistributionPlatformService modDistributionPlatformService;
    private final ProfileResolver profileResolver;
    private final PlatformService platformService;
    private final ModTypeService modTypeService;
    private final Console console;

    @CommandLine.Option(
        names = {"--platform"},
        description = "The mod distribution platform to use.",
        completionCandidates = ModDistributionPlatformCompletions.class
    )
    private @Nullable String platform;

    @CommandLine.Option(
        names = {"-w", "--world"},
        description = "The world to look for datapacks in.",
        completionCandidates = WorldCompletions.class
    )
    private @Nullable String world;

    @CommandLine.Parameters(
        description = "The type of mod to install, e.g. 'mod', 'resourcepack', 'modpack', 'datapack', 'shader'.",
        paramLabel = "type",
        index = "0",
        arity = "1",
        completionCandidates = ModTypeCompletions.class
    )
    private @Nullable String type;

    @CommandLine.Parameters(
        description = "The mod to add.",
        paramLabel = "mod",
        index = "1",
        arity = "1"
    )
    private @Nullable String mod;

    @CommandLine.Parameters(
        paramLabel = "version",
        description = "The version/profile to add the mod to.",
        index = "2",
        arity = "0..*",
        completionCandidates = ProfileVersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        String mod = this.mod;
        if (mod == null) {
            throw new IllegalArgumentException("Please specify the mod to download.");
        }

        String type = this.type;
        if (type == null) {
            throw new IllegalArgumentException(
                "Please specify the type of the mod to download ('mod', 'resourcepack', 'datapack', ...)."
            );
        }

        if (versionArg.isEmpty()) {
            throw new IllegalArgumentException("Please specify a profile/version to add the mod to.");
        }

        String world = this.world;
        ModDistributionPlatform distributionPlatform = modDistributionPlatformService.getByArg(platform);
        Profile profile = profileResolver.resolve(versionArg, Side.BOTH);
        Platform platform = platformService.getPlatform(profile.version().platform())
            .orElseThrow(() -> new IllegalArgumentException("Failed to find platform " + profile.version().platform() + " for profile " + profile.name()));

        ModType modType = modTypeService.getByName(type)
            .orElseThrow(() -> new IllegalArgumentException(
                "Failed to find mod type " + type + ", available: " + modTypeService.getAllModTypes()
            ));

        if (ModType.MOD.equals(modType) || ModType.PLUGIN.equals(modType)) {
            if (!platform.getModSupport().map(m -> m.modTypes().contains(modType)).orElse(false)) {
                log.warn(
                    "Platform {} does not support mods of type {}{}",
                    platform.getName(),
                    modType,
                    platform.getModSupport().map(m -> ", it supports: " + String.join(
                        ", ",
                        m.modTypes().stream().map(ModType::name).toList()
                    )).orElse("")
                );
            }
        }

        Path worldDir = null;
        if (ModType.DATA_PACK.equals(modType)) {
            if (world == null) {
                throw new IllegalArgumentException("Please specify a world to install the datapack to.");
            }

            worldDir = Worlds.resolveWorld(profile, world);
        }

        Path file = distributionPlatform.download(profile.path(), worldDir, mod, profile.version(), modType);
        console.write("Downloaded " + file + " successfully.");
    }

}
