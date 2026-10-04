package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
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
@Named("command:version")
@CommandLine.Command(
    name = "version",
    aliases = {"download", "install"},
    mixinStandardHelpOptions = true,
    description = "Manage versions.",
    subcommands = {
        InstallCommand.class,
        ListCommand.class
    }
)
public class VersionCommand extends VersionInstallCommand {
    @Inject
    public VersionCommand(
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

}
