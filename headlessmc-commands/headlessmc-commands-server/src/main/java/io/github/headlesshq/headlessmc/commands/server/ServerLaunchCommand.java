package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
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

import java.util.concurrent.Callable;

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
        description = "The name of the server to launch.",
        paramLabel = "server",
        completionCandidates = ServerCompletions.class
    )
    private @Nullable String name;

    @Inject
    public ServerLaunchCommand(
        LifecycleService lifecycleService,
        ProfileResolver profileResolver,
        PlatformService platformService,
        ServerLauncher serverLauncher,
        ServerService serverService,
        ArgSplitter splitter,
        Console console,
        McFiles mcFiles
    ) {
        super(
            lifecycleService,
            profileResolver,
            platformService,
            serverLauncher,
            serverService,
            splitter,
            console,
            mcFiles
        );
    }

    @Override
    public Profile getProfile() {
        String name = this.name;
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the server to launch.");
        }

        Profile server = getServerService().getServer(name).orElse(null);
        if (server == null) {
            throw new IllegalArgumentException("Failed to find server/profile with name " + name);
        } else if (!Side.SERVER.equals(server.side())) {
            throw new IllegalStateException("Found launch profile " + name + " but it targets the client side.");
        }

        return server;
    }

}
