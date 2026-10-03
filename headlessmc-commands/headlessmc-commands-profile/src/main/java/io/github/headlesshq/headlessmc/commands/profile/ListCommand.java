package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.util.List;

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
    private final ProfileService profileService;
    private final TableProvider tableProvider;
    private final Console console;

    @Override
    public void run() {
        List<Profile> profiles = profileService.getProfiles();
        tableProvider.<Profile>get()
            .withColumn("name", Profile::name)
            .withColumn("version", profile -> profile.version().toString())
            .withColumn("current version", profile -> profile.currentVersion().toString())
            //.withColumn("account", Profile::account)
            .withStringValueOf("java", Profile::javaVersion)
            .withColumn("path", profile -> profile.path().toString())
            .addAll(profiles)
            .log(console::write);
    }

}
