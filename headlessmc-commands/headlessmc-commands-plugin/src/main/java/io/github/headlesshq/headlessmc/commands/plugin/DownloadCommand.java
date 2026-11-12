package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.plugins.PluginIndex;
import io.github.headlesshq.headlessmc.plugins.PluginIndexService;
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
import java.util.stream.Collectors;

@Getter
@Setter
@Default
@Dependent
@Named("command:plugin:download")
@CommandLine.Command(
    name = "download",
    aliases = "install",
    mixinStandardHelpOptions = true,
    description = "Downloads an officially supported plugin into the plugins directory."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DownloadCommand implements Runnable, CachedConsole.Enabled {
    private final PluginIndexService indexService;
    private final AppFiles appFiles;
    private final Console console;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Installs the version even if it is not compatible with this version of HeadlessMc.",
        defaultValue = "false"
    )
    private boolean force;

    @CommandLine.Parameters(
        description = "The id of the plugin, see 'plugin search'.",
        paramLabel = "id",
        index = "0",
        arity = "1"
    )
    private @Nullable String id;

    @CommandLine.Parameters(
        description = "The version of the plugin, by default the newest version compatible with this version of HeadlessMc.",
        paramLabel = "version",
        index = "1",
        arity = "0..1"
    )
    private @Nullable String version;

    @Override
    public void run() {
        String id = this.id;
        if (id == null || !PluginIndexService.isValidId(id)) {
            throw new IllegalArgumentException("Invalid plugin id '" + id + "', plugin ids consist of lowercase letters, digits, - and _.");
        }

        String version = this.version;
        if (version != null && !PluginIndexService.isValidVersion(version)) {
            throw new IllegalArgumentException("Invalid version '" + version + "', versions consist of letters, digits, ., +, - and _.");
        }

        PluginIndex index = indexService.load();
        PluginIndex.Entry plugin = index.find(id).orElseThrow(() -> new IllegalArgumentException(
            "Unknown plugin '" + id + "', use 'plugin search' to find the available plugins."));
        PluginIndex.Release release = select(plugin, version);
        if (!indexService.isCompatible(release)) {
            console.write("Warning: " + id + " " + release.version() + " is not compatible with HeadlessMc "
                + indexService.getHeadlessMcVersion() + ", it needs " + release.headlessmc() + ".");
        }

        Path pluginDir = appFiles.getPluginDir();
        PluginIndexService.Installation installation = indexService.install(plugin, release, pluginDir);
        console.write(installation.downloaded()
            ? "Downloaded " + plugin.displayName() + " " + release.version() + " to " + installation.file() + "."
            : plugin.displayName() + " " + release.version() + " is already installed at " + installation.file() + ".");
        for (Path removed : installation.removed()) {
            console.write("Removed " + removed.getFileName() + ".");
        }

        if (installation.downloaded() || !installation.removed().isEmpty()) {
            console.write("The plugin will be loaded the next time HeadlessMc starts,"
                + " which rebuilds HeadlessMc with the installed plugins once, this takes a few seconds.");
        }
    }

    private PluginIndex.Release select(PluginIndex.Entry plugin, @Nullable String version) {
        if (version != null) {
            PluginIndex.Release release = plugin.release(version).orElseThrow(() -> new IllegalArgumentException(
                "Plugin " + plugin.id() + " has no version " + version + ", available: " + available(plugin) + "."));
            if (!force && !indexService.isCompatible(release)) {
                throw new IllegalArgumentException(plugin.id() + " " + version + " is not compatible with HeadlessMc "
                    + indexService.getHeadlessMcVersion() + ", it needs " + release.headlessmc()
                    + ". Use --force to install it anyway.");
            }

            return release;
        }

        return indexService.newestCompatible(plugin).orElseGet(() -> {
            PluginIndex.Release newest = indexService.newest(plugin).orElseThrow(() -> new IllegalArgumentException(
                "Plugin " + plugin.id() + " has no downloads."));
            if (!force) {
                throw new IllegalArgumentException("No version of " + plugin.id() + " is compatible with HeadlessMc "
                    + indexService.getHeadlessMcVersion() + ", available: " + available(plugin)
                    + ". Use --force to install the newest version anyway.");
            }

            return newest;
        });
    }

    private String available(PluginIndex.Entry plugin) {
        String available = indexService.sorted(plugin).stream()
            .map(release -> release.version() + " (HeadlessMc " + release.headlessmc() + ")")
            .collect(Collectors.joining(", "));
        return available.isEmpty() ? "none" : available;
    }

}
