package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.commands.util.ProfileVersionCompletions;
import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableBuilder;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.ModTypeService;
import io.github.headlesshq.headlessmc.mods.distribution.CompositeDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatformService;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.platform.PlatformService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Default
@Dependent
@Named("command:mod:search")
@CommandLine.Command(
    name = "search",
    mixinStandardHelpOptions = true,
    description = "Searches for mods on a distribution platform."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class SearchCommand implements Runnable, VersionArgCommand, CachedConsole.Enabled {
    private final ModDistributionPlatformService modDistributionPlatformService;
    private final ProfileResolver profileResolver;
    private final PlatformService platformService;
    private final ModTypeService modTypeService;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Option(
        names = {"-t", "--type"},
        description = "The type of mod to search for, e.g. 'mod', 'resourcepack', 'modpack', 'datapack', 'shader'.",
        completionCandidates = ModTypeCompletions.class
    )
    private @Nullable String type;

    @CommandLine.Option(
        names = {"--platform"},
        description = "The mod distribution platform to use, 'default' (GitHub and Modrinth), 'all', or a specific platform.",
        completionCandidates = ModDistributionPlatformCompletions.class
    )
    private @Nullable String platform;

    @CommandLine.Parameters(
        description = "The query to search for.",
        paramLabel = "query",
        index = "0",
        arity = "1"
    )
    private @Nullable String query;

    @CommandLine.Parameters(
        paramLabel = "version",
        description = "The version/profile to use",
        index = "1",
        arity = "0..*",
        completionCandidates = ProfileVersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        String query = this.query;
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Please specify a query to search for.");
        }

        ModDistributionPlatform distributionPlatform = modDistributionPlatformService.getByArg(platform);
        VersionArg version = null;
        ModType modType;
        if (versionArg.isEmpty()) {
            modType = Mods.resolveType(platformService, modTypeService.getAllModTypes(), null, type);
        } else {
            Profile resolved = profileResolver.resolve(versionArg, Side.BOTH);
            modType = Mods.resolveType(platformService, modTypeService.getAllModTypes(), resolved, type);
            version = resolved.version();
        }

        Map<String, List<RemoteMod>> results;
        if (distributionPlatform instanceof CompositeDistributionPlatform composite) {
            results = composite.searchByPlatform(query, version, modType);
        } else {
            results = Map.of(distributionPlatform.getName(), version == null
                ? distributionPlatform.search(query, modType)
                : distributionPlatform.search(query, version, modType));
        }

        TableBuilder<Result> table = tableProvider.<Result>get()
            .withColumn("id", result -> result.mod().id())
            .withColumn("name", result -> result.mod().name());
        if (results.size() > 1) {
            table = table.withColumn("platform", Result::platform);
        }

        table.withColumn("description", result -> result.mod().description())
            .addAll(results.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream().map(mod -> new Result(entry.getKey(), mod)))
                .toList())
            .log(console::write);
    }

    private record Result(String platform, RemoteMod mod) {

    }

}
