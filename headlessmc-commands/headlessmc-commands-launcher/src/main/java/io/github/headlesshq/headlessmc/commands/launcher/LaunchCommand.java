package io.github.headlesshq.headlessmc.commands.launcher;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.commands.profile.AbstractClientAndServerLaunchCommand;
import io.github.headlesshq.headlessmc.commands.profile.XvfbService;
import io.github.headlesshq.headlessmc.commands.util.LaunchTargetCommand;
import io.github.headlesshq.headlessmc.commands.util.LaunchTargetCompletions;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.client.PackwizLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
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
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Launches a stored profile by its name, or anything a launch target can launch:
 * <pre>
 * launch main                          the profile named main
 * launch fabric 1.21.1                 a version
 * launch server paper 1.21.1           a server
 * launch ~/versions/pack/pack.json     a version.json file
 * launch --target jar ./tool.jar       a jar, naming the launch target explicitly
 * </pre>
 * A single argument that is the name of a stored profile always launches that profile.
 * Everything else is given to the launch targets, see {@link ProfileResolver}: if a profile for exactly these
 * arguments is stored it is used, otherwise a profile is created for this launch only.
 */
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
public class LaunchCommand extends AbstractClientAndServerLaunchCommand implements LaunchTargetCommand {
    @CommandLine.Option(
        names = {"--packwiz"},
        description = "Path to a pack.toml packwiz modpack file, or a directory containing one, to launch. "
            + "The same as '--target packwiz <path>'."
    )
    private @Nullable String packwiz;

    @CommandLine.Parameters(
        description = "The profile to launch, or what to launch, e.g. 'fabric 1.21.1'.",
        paramLabel = "profile",
        arity = "0..*",
        completionCandidates = LaunchTargetCompletions.class
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
        LaunchTargetService launchTargetService,
        McFilesService mcFilesService,
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
            launchTargetService,
            mcFilesService,
            lastUsedAccountService,
            xvfbService,
            authService
        );
    }

    @Override
    public Profile getProfile() {
        String packwiz = this.packwiz;
        if (packwiz != null) {
            if (getTarget() != null && !PackwizLaunchTarget.NAME.equalsIgnoreCase(getTarget())) {
                throw new IllegalArgumentException("--packwiz cannot be combined with --target " + getTarget());
            }

            List<String> args = new ArrayList<>(versionArg.size() + 1);
            args.add(packwiz);
            args.addAll(versionArg);
            return getProfileResolver().resolve(PackwizLaunchTarget.NAME, args, Side.BOTH, getTargetOptions());
        }

        if (versionArg.isEmpty()) {
            throw new IllegalArgumentException("Specify version to launch.");
        }

        return getProfileResolver().resolve(getTarget(), versionArg, Side.BOTH, getTargetOptions());
    }

    @Override
    public @Nullable List<String> getTargetArgs() {
        return versionArg;
    }

}
