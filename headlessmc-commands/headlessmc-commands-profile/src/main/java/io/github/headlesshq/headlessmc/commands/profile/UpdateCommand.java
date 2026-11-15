package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.Objects;

/**
 * Updates what a profile launches.
 * A profile remembers what it has been resolved to, e.g. a profile for {@code fabric 1.21.1 latest}
 * keeps launching the build that was the latest when it was launched first.
 * This command makes its launch target forget that and resolve again,
 * installing the version it resolves to now if needed.
 * With {@code --mc} the profile is moved to the version in another Minecraft directory instead.
 */
@Getter
@Setter
@Default
@Dependent
@Named("command:profile:update")
@CommandLine.Command(
    name = "update",
    mixinStandardHelpOptions = true,
    description = "Resolves what a profile launches again, e.g. to move it to the latest build."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UpdateCommand implements Runnable, LongRunningCommand {
    private final LaunchTargetService launchTargetService;
    private final ProfileService profileService;
    private final McFilesService mcFilesService;
    private final FileService fileService;
    private final Console console;

    @CommandLine.Option(
        names = {"--mc"},
        description = "The Minecraft directory to launch the profile from, the version is installed into it if needed."
    )
    private @Nullable String mc;

    @CommandLine.Parameters(
        paramLabel = "profile",
        description = "The name of the profile to update.",
        completionCandidates = ProfileCompletions.class
    )
    private @Nullable String name;

    @Override
    public void run() {
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the profile to update.");
        }

        Profile profile = profileService.getProfile(name)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find profile with name " + name));

        TypedMap options = new TypedMapImpl();
        options.put(LaunchTarget.UPDATE, true);
        if (mc != null) {
            options.put(LaunchTarget.MC_FILES, mcFilesService.of(fileService.getUserPath(mc)));
        }

        TargetInfo before = launchTargetService.describe(profile);
        Profile updated = launchTargetService.resolveAndPersist(profile, options);
        TargetInfo after = launchTargetService.describe(updated);
        if (Objects.equals(before, after)) {
            console.write("Profile " + profile.name() + " is up to date: " + describe(after));
        } else {
            console.write("Updated profile " + profile.name() + ": " + describe(before) + " -> " + describe(after));
        }
    }

    private static String describe(TargetInfo info) {
        String version = info.version().map(Object::toString).orElse("-");
        return info.file().map(file -> version + " (" + file + ")").orElse(version);
    }

}
