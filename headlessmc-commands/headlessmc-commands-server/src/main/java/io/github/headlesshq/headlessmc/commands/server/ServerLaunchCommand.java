package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
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
import java.util.Set;

@Getter
@Setter
@Default
@Dependent
@Named("command:server:launch")
@CommandLine.Command(
    name = "launch",
    mixinStandardHelpOptions = true,
    description = "Launches a server."
)
public class ServerLaunchCommand extends AbstractServerLaunchCommand {
    @CommandLine.Option(
        names = {"-g", "--game"},
        description = "Arguments for the started server"
    )
    private @Nullable String gameArgs;

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        description = "The name of the server to launch. With --target it is not a name, "
            + "but the first argument for the launch target.",
        paramLabel = "server",
        completionCandidates = ServerCompletions.class
    )
    private @Nullable String name;

    @CommandLine.Parameters(
        index = "1..*",
        arity = "0..*",
        description = "Instead of a name: what the server to launch has been added for, "
            + "e.g. 'server launch --target server paper 1.21.1'.",
        paramLabel = "target-args"
    )
    private List<String> targetArgs = new ArrayList<>();

    @Inject
    public ServerLaunchCommand(
        LifecycleService lifecycleService,
        ProfileResolver profileResolver,
        PlatformService platformService,
        ServerLauncher serverLauncher,
        ServerService serverService,
        ArgSplitter splitter,
        Console console,
        LaunchTargetService launchTargetService,
        McFilesService mcFilesService,
        FileService fileService
    ) {
        super(
            lifecycleService,
            profileResolver,
            platformService,
            serverLauncher,
            serverService,
            splitter,
            console,
            launchTargetService,
            mcFilesService,
            fileService
        );
    }

    @Override
    public Profile getProfile() {
        String name = this.name;
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the server to launch.");
        }

        return getProfile(name, targetArgs, Set.of(Side.SERVER), this::getServer);
    }

    private Profile getServer(String name) {
        Profile server = getServerService().getServer(name).orElse(null);
        if (server == null) {
            throw new IllegalArgumentException("Failed to find server/profile with name " + name);
        } else if (!server.isServer()) {
            throw new IllegalStateException("Found launch profile " + name + " but it is not a server.");
        }

        return server;
    }

}
