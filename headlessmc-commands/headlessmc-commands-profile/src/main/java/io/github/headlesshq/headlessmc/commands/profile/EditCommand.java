package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.commands.server.VmArgsUtil;
import io.github.headlesshq.headlessmc.console.*;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.commands.util.LaunchTargetNameCompletions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMover;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@Setter
@Default
@Dependent
@Named("command:profile:edit")
@CommandLine.Command(
    name = "edit",
    mixinStandardHelpOptions = true,
    description = "Edits a profile"
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class EditCommand implements Runnable, CachedConsole.Enabled {
    private final EditFieldHelper editFieldHelper;
    private final ProfileService profileService;
    private final ProfileMover profileMover;
    private final TableProvider tableProvider;
    private final ArgSplitter splitter;
    private final Console console;

    @CommandLine.Option(
        names = {"-t", "--target"},
        description = "For the field 'target': the launch target that handles the new value, e.g. version, jar or profile. "
            + "If it is not specified, it is detected from the value.",
        completionCandidates = LaunchTargetNameCompletions.class
    )
    private @Nullable String target;

    @CommandLine.Option(
        names = {"--move"},
        negatable = true,
        description = "For the field 'path': whether to move the game directory to the new path, like 'profile move', "
            + "or to only make the profile use the new path. If it is not specified and there is a game directory "
            + "to move and nothing at the new path yet, you are asked."
    )
    private @Nullable Boolean move;

    @CommandLine.Parameters(
        paramLabel = "profile",
        index = "0",
        //arity = "1",
        description = "The name of the profile to edit.",
        completionCandidates = ProfileCompletions.class
    )
    private @Nullable String name;

    @CommandLine.Parameters(
        paramLabel = "field",
        index = "1",
        arity = "0..1",
        description = "The field of the profile to edit.",
        completionCandidates = ProfileFieldCompletions.class
    )
    private @Nullable String field;

    @CommandLine.Parameters(
        paramLabel = "value",
        index = "2",
        arity = "0..1",
        description = "The value to set the profile field to (optional)."
        // completionCandidates = ProfileFieldValueCompletions.class
    )
    private @Nullable String value;

    @Override
    public void run() {
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the profile to edit.");
        }

        Profile profile = profileService.getProfile(name)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find profile with name " + name));

        SequencedMap<String, EditField<?>> fields = editFieldHelper.fields(profile, target);
        if (field == null) {
            console.write("Specify one of the fields to edit for profile " + profile.name() + ":");
            tableProvider.<String>get()
                .addAll(fields.keySet())
                .withColumn("name", Function.identity())
                .withColumn("value", key -> fields.get(key).getStringValue(profile))
                .log(console::write);
            return;
        }

        EditField<?> fieldToEdit = fields.get(field);
        if (fieldToEdit == null) {
            throw new IllegalArgumentException(
                "Failed to find field " + field + ", available: " + String.join(", ", fields.keySet()) + "."
            );
        }

        String value = this.value;
        if (value != null) {
            finishEdit(fieldToEdit, value, profile);
            return;
        }

        ConsoleExtensions extensions = console.extensions()
            .orElseThrow(() -> new ConsoleException(
                "Editing is not possible in this terminal. Use Jline (hmc.jline.enabled=true) for editing."
            ));

        edit(fieldToEdit, extensions, profile);
    }

    private <T> void edit(EditField<T> field, ConsoleExtensions extensions, Profile profile) {
        String current = field.edit.apply(field.getter.apply(profile));
        String newValue = extensions.edit(current);
        finishEdit(field, newValue, profile);
    }

    private <T> void finishEdit(EditField<T> field, String newValue, Profile profile) {
        T parsed = field.parse.apply(newValue);
        Profile newProfile = field.setter.apply(profile, parsed);
        if (!newProfile.name().equals(profile.name())) {
            // a rename is more than a new name: profiles inheriting from the profile and its backups follow it
            profileMover.move(new ProfileMover.Request(profile, null, newProfile.name(), false, false));
            return;
        }

        if (!isSameDirectory(profile.path(), newProfile.path()) && moveGameDirectory(profile, newProfile.path())) {
            return;
        }

        profileService.save(newProfile);
    }

    /**
     * A new path only makes a profile use another game directory. If the profile has a game directory and
     * there is nothing at the new path, that would start a new, empty game and leave the old one behind,
     * which is rarely what is wanted: the user is offered to move the game directory instead.
     *
     * @return {@code true} if the game directory has been moved, the profile has been saved with its new path then.
     */
    private boolean moveGameDirectory(Profile profile, Path newPath) {
        @Nullable Boolean move = this.move;
        if (move == null) {
            if (!hasFiles(profile.path()) || hasFiles(newPath) || Files.isRegularFile(newPath)) {
                // nothing to move, or the new path is an existing game directory the profile is pointed at
                return false;
            }

            String answer = console.read(
                "The game directory " + profile.path() + " of profile " + profile.name() + " exists, but there is nothing at "
                    + newPath + " yet. Move the game directory there? [y/N] "
            );
            //noinspection ConstantValue
            move = answer != null && List.of("y", "yes").contains(answer.trim().toLowerCase(Locale.ENGLISH));
            if (!move) {
                console.write(
                    "Profile " + profile.name() + " now uses " + newPath + ", the files in " + profile.path()
                        + " have not been moved. Use 'profile move " + profile.name() + " <directory>' to move them."
                );
            }
        }

        if (!move) {
            return false;
        }

        ProfileMover.Result result;
        try {
            result = profileMover.move(new ProfileMover.Request(profile, newPath, null, false, false));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                e.getMessage() + " Nothing has been changed, see 'profile move --help' for more ways to move a profile.", e
            );
        }

        console.write(
            result.filesMoved()
                ? "Moved the game directory of profile " + profile.name() + " from " + profile.path() + " to " + result.after().path()
                : "Profile " + profile.name() + " now uses " + result.after().path()
        );
        return true;
    }

    private static boolean isSameDirectory(Path a, Path b) {
        return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }

    /**
     * @return {@code true} if the path is a directory that is not empty.
     */
    private static boolean hasFiles(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }

        try (Stream<Path> files = Files.list(path)) {
            return files.findAny().isPresent();
        } catch (IOException e) {
            // cannot look inside: do not offer to move into or out of it
            return true;
        }
    }

    record EditField<T>(
        Function<Profile, T> getter,
        Function<T, String> edit,
        Function<String, T> parse,
        BiFunction<Profile, T, Profile> setter
        // TODO: with Completer?!
    ) {
        String getStringValue(Profile profile) {
            return edit.apply(getter.apply(profile));
        }
    }

}
