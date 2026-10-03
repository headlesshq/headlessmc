package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
@Setter
@Default
@Dependent
@Named("command:profile:remove")
@CommandLine.Command(
    name = "remove",
    aliases = {"rm"},
    mixinStandardHelpOptions = true,
    description = "Removes a profile."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand implements Runnable, CachedConsole.Enabled {
    private final ProfileService profileService;
    private final Console console;

    @CommandLine.Parameters(
        paramLabel = "profile",
        description = "The name of the profile to remove.",
        completionCandidates = ProfileCompletions.class
    )
    private @Nullable String name;

    @Override
    public void run() {
        if (name == null) {
            throw new IllegalArgumentException("Please specify the name of the profile to remove.");
        }

        Profile profile = profileService.getProfile(name)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find profile with name " + name));

        profileService.remove(profile);
        console.write("Removed profile " + profile.name());
    }

}
