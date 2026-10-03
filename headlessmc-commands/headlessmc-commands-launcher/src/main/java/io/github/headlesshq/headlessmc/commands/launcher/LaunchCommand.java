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
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

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
    @CommandLine.Parameters(
        description = "The profile/server/version to launch",
        paramLabel = "profile",
        arity = "1..*",
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
        AuthService authService
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
    }


    @Override
    public Profile getProfile() {
        if (versionArg.isEmpty()) {
            throw new IllegalArgumentException("Specify version to launch.");
        }

        return getProfileResolver().resolve(versionArg, Side.BOTH);
    }

}
