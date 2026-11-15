package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMover;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.Optional;

/**
 * Moves the directory of a server, and/or renames the server.
 * <pre>
 * server move lobby /srv/minecraft/lobby   moves the directory of the server
 * server move lobby --name hub             only renames the server
 * </pre>
 * This is {@code profile move} for servers, it does not move profiles that are not servers.
 *
 * @see ProfileMover
 */
@Getter
@Setter
@Default
@Dependent
@Named("command:server:move")
@CommandLine.Command(
    name = "move",
    aliases = {"mv", "rename"},
    mixinStandardHelpOptions = true,
    description = "Moves the directory of a server and/or renames the server."
)
public class MoveCommand extends AbstractMoveCommand {
    private final ProfileService profileService;
    private final ServerService serverService;

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        paramLabel = "SERVER",
        description = "The name of the server to move.",
        completionCandidates = ServerCompletions.class
    )
    private @Nullable String name;

    @Inject
    public MoveCommand(
        LaunchTargetService launchTargetService,
        ProfileService profileService,
        ServerService serverService,
        ProfileMover profileMover,
        FileService fileService,
        Console console
    ) {
        super(launchTargetService, profileMover, fileService, console);
        this.profileService = profileService;
        this.serverService = serverService;
    }

    /**
     * A server never replaces a profile that is not a server, not even with {@code --force}.
     */
    @Override
    public void run() {
        @Nullable String newName = getNewName();
        if (newName != null) {
            Optional<Profile> existing = profileService.getProfile(newName).filter(profile -> !profile.isServer());
            if (existing.isPresent()) {
                throw new IllegalArgumentException(existing.get().isClient()
                    ? "A client profile with name " + existing.get().name() + " already exists."
                    : "A profile with name " + existing.get().name() + " that is not a server already exists.");
            }
        }

        super.run();
    }

    @Override
    protected String getKind() {
        return "server";
    }

    @Override
    protected Profile findProfile(String name) throws IllegalArgumentException {
        return serverService.getServer(name)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find server with name " + name));
    }

}
