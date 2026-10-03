package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.commands.server.VmArgsUtil;
import io.github.headlesshq.headlessmc.console.*;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.BiFunction;
import java.util.function.Function;

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
    private final TableProvider tableProvider;
    private final ArgSplitter splitter;
    private final Console console;

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
            throw new IllegalArgumentException("Please specify the name of the profile to remove.");
        }

        Profile profile = profileService.getProfile(name)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find profile with name " + name));

        SequencedMap<String, EditField<?>> fields = editFieldHelper.fields(profile);
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
        profileService.save(newProfile);
        if (!newProfile.name().equals(profile.name())) {
            profileService.remove(profile);
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
