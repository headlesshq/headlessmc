package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.client.VersionMcFilesService;
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

/**
 * For legacy: In HeadlessMc 2 there was a download/install command.
 */
@Getter
@Setter
@Default
@Dependent
@Named("command:install")
@CommandLine.Command(
    name = "install",
    aliases = "download",
    hidden = true,
    mixinStandardHelpOptions = true,
    description = "Downloads and installs a version."
)
public class InstallShortcutCommand extends VersionInstallCommand {
    @Inject
    public InstallShortcutCommand(
        PlatformService platformService,
        ProfileService profileService,
        ServerService serverService,
        FileService fileService,
        AppFiles appFiles,
        VersionMcFilesService versionMcFilesService,
        Console console
    ) {
        super(platformService, profileService, serverService, fileService, appFiles, versionMcFilesService, console);
    }

}
