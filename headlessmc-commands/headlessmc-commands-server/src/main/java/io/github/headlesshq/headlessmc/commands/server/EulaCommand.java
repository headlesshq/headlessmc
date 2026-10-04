package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.Eula;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@Default
@Dependent
@Named("command:server:eula")
@CommandLine.Command(
    name = "eula",
    mixinStandardHelpOptions = true,
    description = "Manage the EULA of a server.",
    subcommands = {
        EulaCommand.AcceptCommand.class,
        EulaCommand.ReadCommand.class
    }
)
@RequiredArgsConstructor
public class EulaCommand implements CachedConsole.Enabled {
    final ProfileResolver profileResolver;
    final ServerLauncher serverLauncher;
    final ServerService serverService;
    final Console console;

    Profile getProfile(@Nullable String name) {
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the server to handle the EULA of.");
        }

        return profileResolver.resolve(List.of(name), Set.of(Side.SERVER));
    }

    // output acceptance status?

    @Getter
    @Setter
    @Default
    @Dependent
    @Named("command:server:eula:read")
    @CommandLine.Command(
        name = "read",
        mixinStandardHelpOptions = true,
        description = "Read the EULA of a server."
    )
    @RequiredArgsConstructor(onConstructor_ = {@Inject})
    public static class ReadCommand implements Runnable, CachedConsole.Enabled {
        @CommandLine.ParentCommand
        private EulaCommand ctx;

        @CommandLine.Parameters(
            description = "The name of the server to read the EULA of.",
            completionCandidates = ServerCompletions.class
        )
        private @Nullable String name;

        @Override
        public void run() {
            Profile profile = ctx.getProfile(name);
            Optional<Eula> eula = ctx.serverLauncher.getEulaLauncher().eulaLaunch(profile, ctx.serverService);
            if (eula.isPresent()) {
                ctx.console.write(eula.get().read());
            } else {
                ctx.console.write("The server " + profile.name() + " does not have a EULA!");
            }
        }
    }

    @Getter
    @Setter
    @Default
    @Dependent
    @Named("command:server:eula:accept")
    @CommandLine.Command(
        name = "accept",
        mixinStandardHelpOptions = true,
        description = "Accept the EULA of a server."
    )
    @RequiredArgsConstructor(onConstructor_ = {@Inject})
    public static class AcceptCommand implements Runnable, CachedConsole.Enabled {
        @CommandLine.ParentCommand
        private EulaCommand ctx;

        @CommandLine.Parameters(
            description = "The name of the server to accept the EULA of.",
            completionCandidates = ServerCompletions.class
        )
        private @Nullable String name;

        @Override
        public void run() {
            Profile profile = ctx.getProfile(name);
            Optional<Eula> eula = ctx.serverLauncher.getEulaLauncher().eulaLaunch(profile, ctx.serverService);
            if (eula.isPresent()) {
                eula.get().accept();
                ctx.console.write("Accepted the EULA of " + profile.name());
            } else {
                ctx.console.write("The server " + profile.name() + " does not have a EULA!");
            }
        }
    }

}
