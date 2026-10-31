package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.FileService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Speeds up the start of the JVM running HeadlessMc with plugins with an AOT cache (JEP 483/514, Java 25+),
 * which saves about a third of its startup time.
 * <p>
 * Creating the cache needs a training run and prints output the JVM cannot be told to suppress,
 * so it never happens in the run the user started: after it has exited, {@link #train} starts
 * {@code headlessmc --version} with {@code -XX:AOTCacheOutput} in the background,
 * with its output redirected into {@value #TRAINING_LOG}. The next start moves the finished cache into place.
 * AOT caches only work with the exact JVM they have been created with,
 * so they are keyed by the Java executable, its size and modification time.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PluginAotCache {
    static final String TRAINING_LOG = "training.log";
    /**
     * Printed by the JVM when it has written the AOT cache.
     */
    static final String COMPLETE = "AOTCache creation is complete";
    /**
     * Hides warnings about classes that cannot be cached and about outdated caches, which the JVM then ignores.
     */
    static final String QUIET = "-Xlog:aot=off,cds=off";
    /**
     * A training that has not finished after this long is considered to have failed.
     */
    static final Duration TRAINING_TIMEOUT = Duration.ofMinutes(10);

    private final Holder<PluginConfig> config;
    private final PluginLaunchCache launchCache;
    private final FileService fileService;

    /**
     * @param launch what to launch.
     * @return the JVM arguments to use the AOT cache of the launch, empty if there is none yet.
     */
    public List<String> jvmArgs(PluginLaunchCache.Launch launch) {
        if (!isEnabled(launch)) {
            return List.of();
        }

        try {
            Paths paths = paths(launch);
            if (!Files.isRegularFile(paths.cache()) && isFinished(paths.training())) {
                // replaces caches for other, e.g. outdated, Java installations
                try (Stream<Path> others = Files.list(paths.cache().getParent())) {
                    for (Path other : others.filter(path -> path.toString().endsWith(".aot")).toList()) {
                        Files.deleteIfExists(other);
                    }
                }

                Files.move(paths.training().resolve("cache.aot"), paths.cache(), StandardCopyOption.ATOMIC_MOVE);
                fileService.delete(paths.training());
            }

            if (Files.isRegularFile(paths.cache())) {
                return List.of("-XX:AOTCache=" + paths.cache(), QUIET);
            }
        } catch (IOException | RuntimeException e) {
            log.debug("Not using an AOT cache for {}", launch, e);
        }

        return List.of();
    }

    /**
     * Starts a training run creating the AOT cache in the background, if there is no cache and no training running.
     * The process keeps running after HeadlessMc has exited.
     *
     * @param launch  what to launch.
     * @param command creates the command line (executable first) of a {@code headlessmc --version}
     *                run with the given additional JVM arguments.
     */
    public void train(PluginLaunchCache.Launch launch, Function<List<String>, List<String>> command) {
        if (!isEnabled(launch)) {
            return;
        }

        try {
            Paths paths = paths(launch);
            // a finished training is moved into place by the next jvmArgs call
            if (Files.isRegularFile(paths.cache()) || isFinished(paths.training()) || isRunning(paths.training())) {
                return;
            }

            fileService.delete(paths.training());
            Files.createDirectories(paths.training());
            List<String> jvmArgs = new ArrayList<>();
            jvmArgs.add("-XX:AOTCacheOutput=" + paths.training().resolve("cache.aot"));
            jvmArgs.add(QUIET);
            Process process = new ProcessBuilder(command.apply(jvmArgs))
                .redirectErrorStream(true)
                .redirectOutput(paths.training().resolve(TRAINING_LOG).toFile())
                .start();
            process.getOutputStream().close();
            log.debug("Training the AOT cache {} in process {}", paths.cache(), process.pid());
        } catch (IOException | RuntimeException e) {
            log.debug("Failed to start training the AOT cache for {}", launch, e);
        }
    }

    private boolean isEnabled(PluginLaunchCache.Launch launch) {
        // in CI every run usually starts with an empty cache, so the training would never pay off
        boolean enabled = config.get().aotCache().orElseGet(() -> System.getenv("CI") == null);
        return enabled && launch.javaVersion() >= PluginBootstrap.MIN_JAVA_VERSION;
    }

    private Paths paths(PluginLaunchCache.Launch launch) throws IOException {
        Path executable = launch.javaExecutable().toRealPath();
        String key = launchCache.fingerprint(List.of(executable), null).substring(0, 16) + "-" + launch.javaVersion();
        Path directory = launch.build().resolve("aot");
        return new Paths(directory.resolve(key + ".aot"), directory.resolve(key + ".training"));
    }

    private static boolean isFinished(Path training) throws IOException {
        Path log = training.resolve(TRAINING_LOG);
        return Files.isRegularFile(training.resolve("cache.aot"))
            && Files.isRegularFile(log)
            && Files.readString(log, StandardCharsets.UTF_8).contains(COMPLETE);
    }

    private static boolean isRunning(Path training) throws IOException {
        if (!Files.isDirectory(training) || isFinished(training)) {
            return false;
        }

        FileTime started = Files.getLastModifiedTime(training);
        return started.toInstant().isAfter(Instant.now().minus(TRAINING_TIMEOUT));
    }

    private record Paths(Path cache, Path training) {

    }

}
