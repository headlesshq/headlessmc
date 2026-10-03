package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
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
import io.github.headlesshq.headlessmc.platform.PlatformService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

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
        description = "The name of the profile to launch.",
        paramLabel = "profile",
        completionCandidates = ProfileCompletions.class
    )
    private @Nullable String name;

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
        McFiles mcFiles,
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
            mcFiles,
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

        return profile;
    }

}
