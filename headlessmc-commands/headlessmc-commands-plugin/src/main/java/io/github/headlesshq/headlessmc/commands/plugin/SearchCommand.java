package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.plugins.PluginFile;
import io.github.headlesshq.headlessmc.plugins.PluginIndex;
import io.github.headlesshq.headlessmc.plugins.PluginIndexService;
import io.github.headlesshq.headlessmc.plugins.PluginManifest;
import io.github.headlesshq.headlessmc.plugins.PluginScanner;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
@Default
@Dependent
@Named("command:plugin:search")
@CommandLine.Command(
    name = "search",
    mixinStandardHelpOptions = true,
    description = "Searches the officially supported plugins."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class SearchCommand implements Runnable, CachedConsole.Enabled {
    private final PluginIndexService indexService;
    private final PluginScanner scanner;
    private final TableProvider tableProvider;
    private final AppFiles appFiles;
    private final Console console;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Shows the full descriptions of the plugins.",
        defaultValue = "false"
    )
    private boolean verbose;

    @CommandLine.Parameters(
        description = "Words the id, name, description or authors of a plugin must contain, lists all plugins if empty.",
        paramLabel = "query",
        arity = "0..*"
    )
    private List<String> query = new ArrayList<>();

    @Override
    public void run() {
        PluginIndex index = indexService.load();
        List<PluginIndex.Entry> plugins = index.pluginList().stream().filter(plugin -> plugin.matches(query)).toList();
        if (plugins.isEmpty()) {
            console.write(query.isEmpty()
                ? "The plugin index " + indexService.getIndexUrl() + " does not contain any plugins."
                : "No plugins matching '" + String.join(" ", query) + "' found, use 'plugin search' without a query to list all plugins.");
            return;
        }

        Map<String, String> installed = installedVersions();
        tableProvider.<PluginIndex.Entry>get()
            .withColumn("id", PluginIndex.Entry::id)
            .withColumn("name", PluginIndex.Entry::displayName)
            .withColumn("version", plugin -> indexService.newestCompatible(plugin).map(PluginIndex.Release::version).orElse("-"))
            .withColumn("authors", plugin -> String.join(", ", plugin.authorList()))
            .withColumn("description", plugin -> ListCommand.shorten(plugin.description(), verbose))
            .withColumn("installed", plugin -> installed.getOrDefault(plugin.id(), ""))
            .addAll(plugins)
            .log(console::write);
        console.write("Use 'plugin download <id>' to install a plugin.");
    }

    private Map<String, String> installedVersions() {
        Map<String, String> result = new TreeMap<>();
        for (PluginFile file : scanner.scan(appFiles.getPluginDir())) {
            PluginManifest manifest = file.manifest();
            if (manifest != null) {
                result.merge(manifest.id(), manifest.version(), (first, second) -> first + ", " + second);
            }
        }

        return result;
    }

}
