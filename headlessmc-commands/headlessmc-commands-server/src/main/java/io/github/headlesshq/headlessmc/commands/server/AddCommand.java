package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

import java.util.Set;

/**
 * Adds a server. Only launch targets for servers are used, so a version does not have to start with
 * {@code server} here, and a jar or script is added as a server.
 */
@Getter
@Setter
@Default
@Dependent
@Named("command:server:install")
@CommandLine.Command(
    name = "add",
    aliases = {"install", "download"},
    mixinStandardHelpOptions = true,
    description = "Downloads and installs a new server."
)
public class AddCommand extends AbstractTargetAddCommand {
    @Inject
    public AddCommand(
        LaunchTargetService launchTargetService,
        ProfileService profileService,
        McFilesService mcFilesService,
        FileService fileService,
        Console console
    ) {
        super(launchTargetService, profileService, mcFilesService, fileService, console);
    }

    @Override
    public Set<Side> getSides() {
        return Set.of(Side.SERVER);
    }

    @Override
    protected String getKind() {
        return "server";
    }

    @Override
    protected String getVerb() {
        return "Installed";
    }

    /**
     * A server never replaces a profile that is not a server, this also holds for profiles
     * that are neither client nor server.
     */
    @Override
    protected void checkExisting(Profile existing) throws IllegalArgumentException {
        if (!existing.isServer()) {
            throw new IllegalArgumentException(existing.isClient()
                ? "A client profile with name " + existing.name() + " already exists."
                : "A profile with name " + existing.name() + " that is not a server already exists.");
        }
    }

}
