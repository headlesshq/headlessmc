package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.plugins.PluginBootstrap;
import io.github.headlesshq.headlessmc.plugins.PluginBuilder;
import io.github.headlesshq.headlessmc.plugins.PluginNativeCompiler;
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
import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@Default
@Dependent
@Named("command:plugin:compile")
@CommandLine.Command(
    name = "compile",
    mixinStandardHelpOptions = true,
    description = {
        "Compiles HeadlessMc with the installed plugins into a native executable, which starts much faster.",
        "Uses a local GraalVM 25, or Docker/Podman with a GraalVM image. Has to be run again when the plugins change."
    }
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CompileCommand implements Runnable, CachedConsole.Enabled {
    private final PluginNativeCompiler compiler;
    private final PluginBuilder builder;
    private final Console console;

    @CommandLine.ArgGroup
    private @Nullable ModeOptions mode;

    @CommandLine.Option(
        names = {"--graalvm"},
        paramLabel = "DIR",
        description = "The home directory of the GraalVM 25 to use. Default: GRAALVM_HOME, JAVA_HOME, the PATH, known Java installations."
    )
    private @Nullable Path graalVm;

    @CommandLine.Option(
        names = {"--image"},
        description = "The container image to use. Default: " + PluginNativeCompiler.DEFAULT_IMAGE
    )
    private @Nullable String image;

    @Override
    public void run() {
        String build = PluginBootstrap.getCurrentBuild();
        if (build == null) {
            console.write("HeadlessMc is not running with plugins, install plugins first, see 'plugin ls'.");
            return;
        }

        Instant start = Instant.now();
        Path executable = compiler.compile(builder.getBuildsDir().resolve(build), getMode(), graalVm, image);
        console.write("Compiled " + executable + " in " + Duration.between(start, Instant.now()).toSeconds()
            + " s, HeadlessMc starts it instead of the JVM for as long as the plugins do not change.");
    }

    private PluginNativeCompiler.Mode getMode() {
        if (mode == null) {
            return PluginNativeCompiler.Mode.AUTO;
        }

        return mode.container ? PluginNativeCompiler.Mode.CONTAINER : PluginNativeCompiler.Mode.LOCAL;
    }

    @Getter
    @Setter
    static class ModeOptions {
        @CommandLine.Option(names = "--local", description = "Uses a local GraalVM.", required = true)
        private boolean local;

        @CommandLine.Option(names = "--container", description = "Uses Docker or Podman (Linux only).", required = true)
        private boolean container;
    }

}
