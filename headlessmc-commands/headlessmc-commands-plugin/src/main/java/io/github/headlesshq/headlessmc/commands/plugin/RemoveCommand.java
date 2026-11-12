package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.plugins.PluginFile;
import io.github.headlesshq.headlessmc.plugins.PluginScanner;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

@Getter
@Setter
@Default
@Dependent
@Named("command:plugin:remove")
@CommandLine.Command(
    name = "remove",
    aliases = {"rm", "uninstall"},
    mixinStandardHelpOptions = true,
    description = "Removes an installed plugin from the plugins directory."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoveCommand implements Runnable, CachedConsole.Enabled {
    private final PluginScanner scanner;
    private final AppFiles appFiles;
    private final Console console;

    @CommandLine.Parameters(
        description = "The id of the plugin or the name of its jar, see 'plugin ls'.",
        paramLabel = "id",
        arity = "1"
    )
    private @Nullable String id;

    @Override
    public void run() {
        String id = this.id;
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Specify the id of the plugin to remove, see 'plugin ls'.");
        }

        List<PluginFile> matching = scanner.scan(appFiles.getPluginDir()).stream()
            .filter(file -> id.equals(file.fileName())
                || (file.manifest() != null && id.equals(file.manifest().id())))
            .toList();
        if (matching.isEmpty()) {
            throw new IllegalArgumentException("Plugin '" + id + "' is not installed, use 'plugin ls' to list the installed plugins.");
        }

        for (PluginFile file : matching) {
            try {
                Files.delete(file.path());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to remove " + file.path(), e);
            }

            console.write("Removed " + file.fileName() + ".");
        }

        console.write("The plugin will be unloaded the next time HeadlessMc starts,"
            + " which rebuilds HeadlessMc without it once, this takes a few seconds.");
    }

}
