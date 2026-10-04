package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.commands.util.ProfileVersionCompletions;
import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:mod:worlds")
@CommandLine.Command(
    name = "worlds",
    mixinStandardHelpOptions = true,
    description = "Lists the worlds that datapacks can be added to."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class WorldsCommand implements Runnable, VersionArgCommand, CachedConsole.Enabled {
    private final ProfileResolver profileResolver;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Parameters(
        paramLabel = "version",
        description = "The version/profile to use",
        arity = "0..*",
        completionCandidates = ProfileVersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        if (versionArg.isEmpty()) {
            throw new IllegalArgumentException("Please specify the profile to list the worlds of.");
        }

        Profile resolved = profileResolver.resolve(versionArg, Side.BOTH);
        List<Path> worlds = Worlds.listWorlds(resolved);

        tableProvider.<Path>get()
            .withColumn("world", world -> world.getFileName().toString())
            .withColumn("path", Path::toString)
            .addAll(worlds)
            .log(console::write);
    }

}
