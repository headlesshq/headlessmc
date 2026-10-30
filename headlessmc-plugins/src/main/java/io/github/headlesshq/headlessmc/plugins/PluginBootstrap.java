package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.java.launcher.JavaFinder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Runs HeadlessMc with the installed plugins.
 * <p>
 * The native image and the uber-jar of HeadlessMc are fully augmented by Quarkus and cannot load plugins.
 * If plugins are installed, {@link #delegate(String...)} builds (or reuses) a re-augmented
 * HeadlessMc containing them with the {@link PluginBuilder}, runs it in a new JVM with the same arguments,
 * and returns its exit code. The child JVM is started with {@value #BUILD_PROPERTY},
 * so it does not delegate again.
 * <p>
 * Hashing the plugins and especially discovering the installed Java versions takes time,
 * so the {@link PluginLaunchCache} remembers the build and the Java for the current plugins,
 * and the {@link PluginAotCache} speeds up the start of the child JVM.
 * If {@code plugin compile} has compiled the build into a native executable ({@link PluginNativeCompiler}),
 * that is started instead of a JVM.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PluginBootstrap {
    /**
     * Set to the hash of the build in a JVM running a build with plugins.
     */
    public static final String BUILD_PROPERTY = "headlessmc.plugins.build";
    /**
     * HeadlessMc requires Java 25.
     */
    public static final int MIN_JAVA_VERSION = 25;

    private final Holder<PluginConfig> config;
    private final PluginScanner scanner;
    private final PluginResolver resolver;
    private final PluginBuilder builder;
    private final MutableJarProvider mutableJarProvider;
    private final JavaFinder javaFinder;
    private final JavaLauncherService javaLauncherService;
    private final PluginLaunchCache launchCache;
    private final PluginAotCache aotCache;
    private final PluginNativeCompiler nativeCompiler;
    private final AppFiles appFiles;

    /**
     * @return the hash of the build this JVM is running, or {@code null} if it is not running with plugins.
     */
    public static @Nullable String getCurrentBuild() {
        return System.getProperty(BUILD_PROPERTY);
    }

    /**
     * Runs HeadlessMc with plugins in a new JVM, if plugins are installed and this JVM is not running with them.
     *
     * @param args the arguments HeadlessMc has been started with.
     * @return the exit code of the JVM running with plugins, or empty if HeadlessMc should continue in this JVM.
     */
    public OptionalInt delegate(String... args) {
        if (getCurrentBuild() != null || !config.get().enabled()) {
            return OptionalInt.empty();
        }

        List<Path> jars = scanner.list(appFiles.getPluginDir());
        if (jars.isEmpty()) {
            return OptionalInt.empty();
        }

        // fast path: the same plugins as last time, no hashing, resolution or Java discovery needed
        Path mutableJar = config.get().mutableJar().map(Path::of).orElse(null);
        String fingerprint = launchCache.fingerprint(jars, mutableJar);
        Optional<PluginLaunchCache.Launch> cached = launchCache.get(fingerprint);
        if (cached.isPresent()) {
            return OptionalInt.of(launch(cached.get(), args));
        }

        List<PluginFile> files = new ArrayList<>(jars.size());
        for (Path jar : jars) {
            files.add(scanner.read(jar));
        }

        PluginResolution resolution = resolver.resolve(files);
        if (!resolution.problems().isEmpty()) {
            for (PluginResolution.Problem problem : resolution.problems()) {
                log.error("Cannot load plugin {}", problem);
            }

            if (config.get().failOnError()) {
                log.error("Fix or remove the plugins in {}, or run with -Dhmc.plugins.fail-on-error=false to start without the broken ones",
                    appFiles.getPluginDir());
                return OptionalInt.of(1);
            }
        }

        if (resolution.plugins().isEmpty()) {
            return OptionalInt.empty();
        }

        Java java = javaFinder.findJava(getJavaVersion(resolution));
        Path build = builder.build(resolution, mutableJarProvider.get(), java);
        PluginLaunchCache.Launch launch = new PluginLaunchCache.Launch(
            build,
            java.home().getUnchecked().orElseThrow(() -> new HeadlessMcException("Java " + java.name() + " has no home")),
            java.executable().getUnchecked().orElseThrow(() -> new HeadlessMcException("Java " + java.name() + " has no executable")),
            java.version()
        );

        // only remembered if all plugins can be loaded, otherwise their problems would not be reported again
        if (resolution.problems().isEmpty()) {
            launchCache.put(fingerprint, launch);
        }

        return OptionalInt.of(launch(launch, args));
    }

    private int launch(PluginLaunchCache.Launch launch, String... args) {
        Path executable = nativeCompiler.getExecutable(launch.build());
        if (config.get().useNative() && Files.isExecutable(executable)) {
            return launchNative(executable, launch, args);
        }

        List<String> aotArgs = aotCache.jvmArgs(launch);
        JavaProcessBuilder process = processBuilder(launch, aotArgs).arg(args);
        log.debug("Running HeadlessMc with plugins: {}", process.buildCommand());
        int exitCode = process.start().waitFor((Duration) null);
        if (aotArgs.isEmpty()) {
            aotCache.train(launch, jvmArgs -> {
                List<String> command = new ArrayList<>();
                command.add(launch.javaExecutable().toString());
                command.addAll(processBuilder(launch, jvmArgs).arg("--version").buildCommand());
                return command;
            });
        }

        return exitCode;
    }

    /**
     * Runs the native executable {@link PluginNativeCompiler} compiled for the build.
     * System properties are passed as {@code -D} arguments before the actual arguments,
     * which HeadlessMcMain parses itself.
     */
    private int launchNative(Path executable, PluginLaunchCache.Launch launch, String... args) {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        Map<String, @Nullable String> properties = new LinkedHashMap<>(getForwardedProperties());
        properties.put(BUILD_PROPERTY, launch.build().getFileName().toString());
        properties.forEach((key, value) -> command.add("-D" + key + (value == null ? "" : "=" + value)));
        command.addAll(List.of(args));
        log.debug("Running native HeadlessMc with plugins: {}", command);
        try {
            return new ProcessBuilder(command).inheritIO().start().waitFor();
        } catch (IOException e) {
            throw new HeadlessMcException("Failed to run " + executable, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HeadlessMcException("Interrupted while running " + executable, e);
        }
    }

    private JavaProcessBuilder processBuilder(PluginLaunchCache.Launch launch, List<String> additionalJvmArgs) {
        Java java = new Java("plugins", launch.javaVersion(), new SafePath(launch.javaHome()),
            new SafePath(launch.javaExecutable()), false, 0);
        List<String> jvmArgs = new ArrayList<>(config.get().jvmArgs().orElse(List.of()));
        jvmArgs.addAll(additionalJvmArgs);
        return javaLauncherService.buildProcess()
            .id("HeadlessMc with plugins")
            .java(java)
            .jvmArg(jvmArgs.toArray(new String[0]))
            .systemProperties(getForwardedProperties())
            .systemProperty(BUILD_PROPERTY, launch.build().getFileName().toString())
            .jar(launch.build().resolve(PluginBuilder.QUARKUS_APP).resolve("quarkus-run.jar"));
    }

    /**
     * @param resolution the resolved plugins.
     * @return the Java version needed to run HeadlessMc with the given plugins.
     */
    static int getJavaVersion(PluginResolution resolution) {
        return resolution.plugins().stream()
            .map(PluginFile::manifest)
            .filter(Objects::nonNull)
            .map(PluginManifest::java)
            .filter(Objects::nonNull)
            .reduce(MIN_JAVA_VERSION, Math::max);
    }

    /**
     * @return the HeadlessMc and Quarkus system properties of this process, e.g. given with {@code -D} on the command line.
     */
    static Map<String, @Nullable String> getForwardedProperties() {
        Map<String, @Nullable String> result = new LinkedHashMap<>();
        System.getProperties().stringPropertyNames().stream()
            .filter(key -> key.startsWith("hmc.") || key.startsWith("quarkus."))
            .filter(key -> !key.equals("quarkus.launch.rebuild"))
            .sorted()
            .forEach(key -> result.put(key, System.getProperty(key)));
        return result;
    }

}
