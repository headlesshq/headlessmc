package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.plugins.PluginBootstrap;
import io.github.headlesshq.headlessmc.plugins.PluginBuilder;
import io.github.headlesshq.headlessmc.plugins.PluginFile;
import io.github.headlesshq.headlessmc.plugins.PluginManifest;
import io.github.headlesshq.headlessmc.plugins.PluginNativeCompiler;
import io.github.headlesshq.headlessmc.plugins.PluginResolution;
import io.github.headlesshq.headlessmc.plugins.PluginResolver;
import io.github.headlesshq.headlessmc.plugins.PluginScanner;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Default
@Dependent
@Named("command:plugin:list")
@CommandLine.Command(
    name = "list",
    aliases = "ls",
    mixinStandardHelpOptions = true,
    description = "Lists the installed plugins."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, CachedConsole.Enabled {
    static final int MAX_DESCRIPTION_LENGTH = 60;

    private final PluginScanner scanner;
    private final PluginResolver resolver;
    private final PluginBuilder builder;
    private final PluginNativeCompiler nativeCompiler;
    private final TableProvider tableProvider;
    private final AppFiles appFiles;
    private final Console console;

    @CommandLine.Option(
        names = {"-l", "--libraries"},
        description = "Lists the libraries of the plugins instead.",
        defaultValue = "false"
    )
    private boolean libraries;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Shows the full descriptions of the plugins.",
        defaultValue = "false"
    )
    private boolean verbose;

    @Override
    public void run() {
        List<PluginFile> files = scanner.scan(appFiles.getPluginDir());
        if (files.isEmpty()) {
            console.write("No plugins installed in " + appFiles.getPluginDir());
            return;
        }

        PluginResolution resolution = resolver.resolve(files);
        if (libraries) {
            listLibraries(resolution);
            return;
        }

        String build = PluginBootstrap.getCurrentBuild();
        Set<String> loaded = build == null ? Set.of() : builder.getPluginHashes(build);
        tableProvider.<PluginFile>get()
            .withColumn("id", file -> file.manifest() == null ? "?" : file.manifest().id())
            .withColumn("version", file -> file.manifest() == null ? "?" : file.manifest().version())
            .withColumn("name", file -> file.manifest() == null ? "" : file.manifest().displayName())
            .withColumn("description", file -> file.manifest() == null ? "" : describe(file.manifest()))
            .withColumn("file", PluginFile::fileName)
            .withColumn("status", file -> status(file, resolution, loaded))
            .addAll(files)
            .log(console::write);
        if (build == null) {
            console.write("HeadlessMc is running without plugins.");
        } else if (Files.isExecutable(nativeCompiler.getExecutable(builder.getBuildsDir().resolve(build)))) {
            console.write("HeadlessMc is running with plugin build " + build + " as a native executable.");
        } else {
            console.write("HeadlessMc is running with plugin build " + build + " on the JVM, 'plugin compile' makes it start faster.");
        }
    }

    private void listLibraries(PluginResolution resolution) {
        tableProvider.<PluginResolution.Library>get()
            .withColumn("library", library -> library.library().module())
            .withColumn("version", library -> library.library().version())
            .withColumn("from", library -> library.source().fileName())
            .withColumn("required by", library -> String.join(", ", library.requestedBy()))
            .addAll(resolution.libraries())
            .log(console::write);
    }

    private String describe(PluginManifest manifest) {
        String description = manifest.description();
        if (description == null) {
            return "";
        }

        return verbose || description.length() <= MAX_DESCRIPTION_LENGTH
            ? description
            : description.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
    }

    private static String status(PluginFile file, PluginResolution resolution, Set<String> loaded) {
        List<PluginResolution.Problem> problems = resolution.problemsOf(file);
        if (!problems.isEmpty()) {
            return "error: " + problems.stream().map(PluginResolution.Problem::message).collect(Collectors.joining("; "));
        }

        if (loaded.contains(file.sha256())) {
            return "loaded";
        }

        PluginManifest manifest = file.manifest();
        return manifest == null ? "error" : "not loaded";
    }

}
