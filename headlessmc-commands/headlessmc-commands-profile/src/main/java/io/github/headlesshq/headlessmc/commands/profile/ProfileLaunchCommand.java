package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:profile:launch")
@CommandLine.Command(
    name = "launch",
    mixinStandardHelpOptions = true,
    description = "Launches a profile."
)
public class ProfileLaunchCommand extends AbstractClientAndServerLaunchCommand {
    private final ProfileService profileService;

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        description = "The name of the profile to launch.",
        paramLabel = "profile",
        completionCandidates = ProfileCompletions.class
    )
    private @Nullable String name;

    @CommandLine.Parameters(
        index = "1..*",
        arity = "0..*",
        description = "With --target: what to launch the profile with instead of its own launch target, once, "
            + "e.g. 'profile launch main --target version fabric 1.21.1 0.16.10'. Nothing is saved.",
        paramLabel = "target-args"
    )
    private List<String> targetArgs = new ArrayList<>();

    @Inject
    public ProfileLaunchCommand(
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
        AuthService authService,
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
            launchTargetService,
            mcFilesService,
            lastUsedAccountService,
            xvfbService,
            authService
        );
        this.profileService = profileService;
    }


    @Override
    public Profile getProfile() {
        String name = this.name;
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the profile to launch.");
        }

        Profile profile = profileService.getProfile(name).orElse(null);
        if (profile == null) {
            throw new IllegalArgumentException("Failed to find profile with name " + name);
        }

        if (getTarget() != null || !targetArgs.isEmpty()) {
            return withTarget(profile, targetArgs, Side.BOTH);
        }

        return profile;
    }

}
