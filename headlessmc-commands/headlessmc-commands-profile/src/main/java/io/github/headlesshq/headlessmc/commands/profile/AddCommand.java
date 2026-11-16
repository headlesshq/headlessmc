package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.commands.server.AbstractTargetAddCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

/**
 * Adds a profile for anything a launch target can launch, e.g.
 * <pre>
 * profile add fabric 1.21.1
 * profile add ./paper.jar --name lobby
 * profile add --target profile main --name main-debug
 * </pre>
 */
@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:profile:add")
@CommandLine.Command(
    name = "add",
    mixinStandardHelpOptions = true,
    description = "Adds a new profile."
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
    protected String getKind() {
        return "profile";
    }

}
