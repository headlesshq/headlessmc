package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.commands.util.ProfileVersionCompletions;
import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:mod:list")
@CommandLine.Command(
    name = "list",
    aliases = {"ls"},
    mixinStandardHelpOptions = true,
    description = "Lists the installed mods of a profile."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, VersionArgCommand, CachedConsole.Enabled {
    private final ModListingService modListingService;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Option(
        names = {"-t", "--type"},
        description = "The type of the mod, e.g. 'mod', 'resourcepack', 'modpack', 'datapack', 'shader'.",
        completionCandidates = ModTypeCompletions.class
    )
    private @Nullable String type;

    @CommandLine.Option(
        names = {"-w", "--world"},
        description = "The world to look for datapacks in.",
        completionCandidates = WorldCompletions.class
    )
    private @Nullable String world;

    @CommandLine.Option(
        names = {"-p", "--platform"},
        description = "Override the profiles platform.",
        completionCandidates = PlatformCompletions.class
    )
    private @Nullable String platform;

    @CommandLine.Parameters(
        description = "The profile to list the mods of.",
        paramLabel = "profile/version",
        index = "0",
        arity = "1..*",
        completionCandidates = ProfileVersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        List<ModFile> files = modListingService.list(versionArg, platform, world, type);
        tableProvider.<ModFile>get()
            .withColumn("id", ModFile::id)
            .withColumn("name", ModFile::name)
            .withColumn("authors", file -> String.join(", ", file.authors()))
            .withColumn("file", file -> file.file().getFileName().toString())
            .withColumn("type", file -> file.type().name())
            .addAll(files)
            .log(console::write);
    }

}
