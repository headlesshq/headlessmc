package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.commands.version.VersionInstallCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

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
public class AddCommand extends VersionInstallCommand {
    @Inject
    public AddCommand(
        PlatformService platformService,
        ProfileService profileService,
        ServerService serverService,
        FileService fileService,
        AppFiles appFiles,
        McFiles mcFiles,
        Console console
    ) {
        super(platformService, profileService, serverService, fileService, appFiles, mcFiles, console);
    }

    @Override
    protected VersionArg arg() {
        VersionArg arg = super.arg();
        if (arg.side().isPresent() && arg.side().get().isClient()) {
            throw new IllegalArgumentException("Cannot install a client version " + arg + " with 'server add'.");
        }

        return arg.withSide(Side.SERVER);
    }

    @Override
    protected Version installClient(Platform platform, VersionID id) {
        throw new UnsupportedOperationException("Cannot install client with server AddCommand");
    }

}
