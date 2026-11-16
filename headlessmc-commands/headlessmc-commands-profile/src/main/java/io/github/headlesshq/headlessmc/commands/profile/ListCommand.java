package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Default
@Dependent
@Named("command:profile:list")
@CommandLine.Command(
    name = "list",
    aliases = {"ls"},
    mixinStandardHelpOptions = true,
    description = "Lists your profiles."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, CachedConsole.Enabled {
    /** Shown where a profile has nothing to show, e.g. the version of a profile launching a jar. */
    private static final String NONE = "-";

    private final LaunchTargetService launchTargetService;
    private final ProfileService profileService;
    private final TableProvider tableProvider;
    private final Console console;

    @Override
    public void run() {
        List<Profile> profiles = profileService.getProfiles();
        tableProvider.<Profile>get()
            .withColumn("name", Profile::name)
            // a profile without a side is neither client nor server
            .withColumn("side", profile -> profile.side()
                .map(side -> side.name().toLowerCase(Locale.ENGLISH))
                .orElse(NONE))
            .withColumn("target", launchTargetService::getLabel)
            .withColumn("version", profile -> toString(launchTargetService.describe(profile).specified().orElse(null)))
            .withColumn("current version", profile -> {
                TargetInfo info = launchTargetService.describe(profile);
                return toString(info.current().orElse(null));
            })
            //.withColumn("account", Profile::account)
            .withStringValueOf("java", Profile::javaVersion)
            .withColumn("path", profile -> profile.path().toString())
            .addAll(profiles)
            .log(console::write);
    }

    private static String toString(@org.jspecify.annotations.Nullable VersionArg version) {
        // the side has its own column
        return version == null ? NONE : version.withSide(null).toString();
    }

}
