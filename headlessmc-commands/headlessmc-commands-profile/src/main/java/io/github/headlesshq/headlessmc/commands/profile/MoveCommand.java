package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.commands.server.AbstractMoveCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMover;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

/**
 * Moves the game directory of a profile, and/or renames the profile.
 * <pre>
 * profile move main /mnt/ssd/games/main      moves the game directory
 * profile move main --name survival          only renames the profile
 * profile move main --mc /mnt/ssd/.minecraft moves the profile to another Minecraft directory
 * </pre>
 * The game directory of a profile is decided when the profile is added, this is the way to change it afterwards
 * without leaving the files behind. {@code profile edit <profile> path} only makes the profile point elsewhere.
 *
 * <p>With {@code --mc} the game directory is moved to where it would be if the profile had been added
 * for that Minecraft directory, and the profile is resolved again for that directory, which installs
 * what it launches there if needed. The whole profile is on the disk of that directory afterwards.</p>
 *
 * @see ProfileMover
 */
@Getter
@Setter
@Default
@Dependent
@Named("command:profile:move")
@CommandLine.Command(
    name = "move",
    aliases = {"mv", "rename"},
    mixinStandardHelpOptions = true,
    description = "Moves the game directory of a profile and/or renames the profile."
)
public class MoveCommand extends AbstractMoveCommand {
    private final ProfileService profileService;
    private final McFilesService mcFilesService;

    @CommandLine.Option(
        names = {"--mc"},
        description = "Moves the profile to another Minecraft directory: its game directory to the default "
            + "location for that directory, unless a directory is given, and what it launches is installed there."
    )
    private @Nullable String mc;

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        paramLabel = "profile",
        description = "The name of the profile to move.",
        completionCandidates = ProfileCompletions.class
    )
    private @Nullable String name;

    @Inject
    public MoveCommand(
        LaunchTargetService launchTargetService,
        ProfileService profileService,
        McFilesService mcFilesService,
        ProfileMover profileMover,
        FileService fileService,
        Console console
    ) {
        super(launchTargetService, profileMover, fileService, console);
        this.profileService = profileService;
        this.mcFilesService = mcFilesService;
    }

    @Override
    protected String getKind() {
        return "profile";
    }

    @Override
    protected String getDestinations() {
        return "a directory, --mc <path> and/or --name <name>";
    }

    @Override
    protected Profile findProfile(String name) throws IllegalArgumentException {
        return profileService.getProfile(name)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find profile with name " + name));
    }

    @Override
    protected @Nullable McFiles getMcFiles() {
        return mc == null ? null : mcFilesService.of(getFileService().getUserPath(mc));
    }

}
