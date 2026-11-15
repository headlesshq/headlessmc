package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMover;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * Base class of the commands that move the game directory of a profile and/or rename the profile,
 * {@code profile move} and {@code server move}. The work is done by the {@link ProfileMover}.
 *
 * <p>The name of the profile to move is a parameter of the subclasses, {@link #getName()},
 * as they complete it differently. It has to be the parameter with index 0.</p>
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractMoveCommand implements Runnable, LongRunningCommand {
    private final LaunchTargetService launchTargetService;
    private final ProfileMover profileMover;
    private final FileService fileService;
    private final Console console;

    @CommandLine.Option(
        names = {"-n", "--name"},
        description = "The new name."
    )
    private @Nullable String newName;

    @CommandLine.Option(
        names = {"--with-shared"},
        description = "Also moves the game directory for the other profiles using it.",
        defaultValue = "false"
    )
    private boolean withShared;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Overwrites a profile that already has the new name.",
        defaultValue = "false"
    )
    private boolean force;

    @CommandLine.Parameters(
        index = "1",
        arity = "0..1",
        paramLabel = "directory",
        description = "The new game directory, it must not exist or be empty."
    )
    private @Nullable String directory;

    /**
     * @return the name of the profile to move, {@code null} if none has been given.
     */
    public abstract @Nullable String getName();

    /**
     * @return what is moved by this command, for messages, e.g. {@code profile}.
     */
    protected abstract String getKind();

    /**
     * @param name the name given to this command.
     * @return the profile to move.
     * @throws IllegalArgumentException if this command does not move a profile with that name.
     */
    protected abstract Profile findProfile(String name) throws IllegalArgumentException;

    /**
     * @return the {@code .minecraft} directory to move the profile to, {@code null} if this command
     * has not been asked to, or does not move profiles between these directories.
     */
    protected @Nullable McFiles getMcFiles() {
        return null;
    }

    /**
     * @return the ways to say where to move to, for the message if none has been given.
     */
    protected String getDestinations() {
        return "a directory and/or --name <name>";
    }

    @Override
    public void run() {
        @Nullable String name = getName();
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the " + getKind() + " to move.");
        }

        @Nullable McFiles mcFiles = getMcFiles();
        if (directory == null && mcFiles == null && newName == null) {
            throw new IllegalArgumentException(
                "Please specify where to move " + getKind() + " " + name + ": " + getDestinations() + "."
            );
        }

        Profile profile = findProfile(name);
        @Nullable Path newPath = directory == null ? null : fileService.getUserPath(directory);
        String targetName = newName == null ? profile.name() : newName;
        if (newPath == null && mcFiles != null) {
            newPath = getDefaultPath(profile, targetName, mcFiles);
        }

        ProfileMover.Result result = profileMover.move(
            new ProfileMover.Request(profile, newPath, newName, withShared, force)
        );

        Profile moved = result.after();
        console.write(describe(result));
        for (Profile updated : result.updated()) {
            console.write("Updated profile " + updated.name());
        }

        if (mcFiles != null) {
            TypedMap options = new TypedMapImpl();
            options.put(LaunchTarget.MC_FILES, mcFiles);
            Profile resolved = launchTargetService.resolveAndPersist(moved, options);
            launchTargetService.describe(resolved).file().ifPresent(
                file -> console.write(capitalize(getKind()) + " " + resolved.name() + " now launches " + file)
            );
        }
    }

    /**
     * The game directory the launch target of the profile would choose for a new profile in the given directory.
     */
    private Path getDefaultPath(Profile profile, String name, McFiles mcFiles) {
        TypedMap options = new TypedMapImpl();
        options.put(LaunchTarget.MC_FILES, mcFiles);
        return launchTargetService.require(profile)
            .create(name, new ParsedTarget(profile.target(), profile.side()), options)
            .path();
    }

    private String describe(ProfileMover.Result result) {
        Profile before = result.before();
        Profile after = result.after();
        StringBuilder message = new StringBuilder();
        if (!before.name().equals(after.name())) {
            message.append("Renamed ").append(getKind()).append(" ").append(before.name()).append(" to ").append(after.name());
        } else {
            message.append("Moved ").append(getKind()).append(" ").append(after.name());
        }

        if (!before.path().toAbsolutePath().normalize().equals(after.path().toAbsolutePath().normalize())) {
            message.append(result.filesMoved() ? ", moved " : ", game directory changed from ")
                .append(before.path())
                .append(" to ")
                .append(after.path());
        }

        return message.toString();
    }

    private static String capitalize(String string) {
        return string.isEmpty() ? string : Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

}
