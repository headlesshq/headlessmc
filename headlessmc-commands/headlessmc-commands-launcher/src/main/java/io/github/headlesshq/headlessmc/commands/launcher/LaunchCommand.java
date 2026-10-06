package io.github.headlesshq.headlessmc.commands.launcher;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.commands.profile.AbstractClientAndServerLaunchCommand;
import io.github.headlesshq.headlessmc.commands.profile.XvfbService;
import io.github.headlesshq.headlessmc.commands.util.ProfileVersionCompletions;
import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.mods.packwiz.PackwizService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@Named("command:launch")
@CommandLine.Command(
    name = "launch",
    description = "Launches the game.",
    version = HeadlessMc.VERSION,
    mixinStandardHelpOptions = true
)
public class LaunchCommand extends AbstractClientAndServerLaunchCommand implements VersionArgCommand {
    private static final String PACK_TOML = "pack.toml";

    private final PackwizService packwizService;
    private final ProfileService profileService;

    @CommandLine.Option(
        names = {"--packwiz"},
        description = "Path to a pack.toml packwiz modpack file, or a directory containing one, to launch."
    )
    private @Nullable String packwiz;

    @CommandLine.Parameters(
        description = "The profile/server/version to launch",
        paramLabel = "profile",
        arity = "0..*",
        completionCandidates = ProfileVersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Inject
    public LaunchCommand(
        LifecycleService lifecycleService,
        ProfileResolver profileResolver,
        PlatformService platformService,
        ServerLauncher serverLauncher,
        ServerService serverService,
        ClientLauncher clientLauncher,
        ArgSplitter splitter,
        Console console,
        McFiles mcFiles,
        LastUsedAccountService lastUsedAccountService,
        XvfbService xvfbService,
        AuthService authService,
        PackwizService packwizService,
        ProfileService profileService
    ) {
        super(
            lifecycleService,
            profileResolver,
            platformService,
            serverLauncher,
            serverService,
            clientLauncher,
            splitter,
            console,
            mcFiles,
            lastUsedAccountService,
            xvfbService,
            authService
        );
        this.packwizService = packwizService;
        this.profileService = profileService;
    }

    @Override
    public Profile getProfile() {
        String packwiz = this.packwiz;
        if (packwiz != null) {
            Path packwizFile = Paths.get(packwiz);
            if (Files.isDirectory(packwizFile)) {
                packwizFile = packwizFile.resolve(PACK_TOML);
            }

            return getPackwizProfile(packwizFile, packwiz)
                .withPath(Objects.requireNonNull(
                    packwizFile.toAbsolutePath().getParent(),
                    "Failed to get directory of " + packwizFile
                ));
        }

        if (versionArg.isEmpty()) {
            throw new IllegalArgumentException("Specify version to launch.");
        }

        return getProfileResolver().resolve(versionArg, Side.BOTH);
    }

    private Profile getPackwizProfile(Path packwizFile, String packwiz) {
        List<VersionID> ids = packwizService.parsePackwizTomlFile(packwizFile);
        if (versionArg.isEmpty()) {
            if (ids.size() > 1) {
                throw new IllegalArgumentException(
                    "Please specify which side (server/client) and loader ("
                        + ids.stream()
                        .map(VersionID::getPlatform)
                        .map(Platform::getName)
                        .collect(Collectors.joining(", "))
                        + ") to use (launch --packwiz " + packwiz.replace(" ", "\\ ") + " <side> <loader>"
                );
            }

            throw new IllegalArgumentException(
                "Please specify which side (server/client)  to use (launch --packwiz "
                    + packwiz.replace(" ", "\\ ") + " <side>"
            );
        }

        if (versionArg.size() == 1) {
            Profile profile = profileService.getProfile(versionArg.getFirst()).orElse(null);
            if (profile != null) {
                return profile;
            }

            Side side = Side.valueOf(versionArg.getFirst().toUpperCase(Locale.ENGLISH));
            if (ids.size() > 1) {
                throw new IllegalArgumentException(
                    "Please specify the loader to use ("
                        + ids.stream()
                        .map(VersionID::getPlatform)
                        .map(Platform::getName)
                        .collect(Collectors.joining(", "))
                        + ")"
                );
            }

            return getProfileService().getProfile(ids.getFirst().asArg().withSide(side));
        }

        if (versionArg.size() == 2) {
            if (getPlatformService().getPlatform(versionArg.getFirst()).isPresent()) {
                // fabric/26.3, side client is implicit
                return getProfileResolver().resolve(versionArg, Side.BOTH);
            }

            Side side = Side.valueOf(versionArg.getFirst().toUpperCase(Locale.ENGLISH));
            List<VersionID> filteredIds = ids.stream()
                .filter(id -> id.getPlatform().getName().equalsIgnoreCase(versionArg.get(1)))
                .toList();
            if (filteredIds.isEmpty()) {
                throw new IllegalArgumentException(
                    "Failed to find loader " + versionArg.get(1) + ", available: "
                        + ids.stream()
                        .map(VersionID::getPlatform)
                        .map(Platform::getName)
                        .collect(Collectors.joining(", "))
                );
            } else if (filteredIds.size() > 1) {
                throw new IllegalStateException(
                    "Multiple ids with loader " + versionArg.get(1) + ": " + filteredIds
                );
            }

            return getProfileService().getProfile(filteredIds.getFirst().asArg().withSide(side));
        }

        return getProfileResolver().resolve(versionArg, Side.BOTH);
    }

}
