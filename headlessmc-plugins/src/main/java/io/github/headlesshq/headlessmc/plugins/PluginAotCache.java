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
import java.util.Optional;
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
 * If a training fails, a {@code .failed} marker containing its log prevents
 * further trainings for the same Java and build.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PluginAotCache {
    static final String TRAINING_LOG = "training.log";
    /**
     * Contains the process id of the training JVM.
     */
    static final String TRAINING_PID = "training.pid";
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
            if (Files.isRegularFile(paths.cache()) || Files.exists(paths.failed()) || isFinished(paths.training())) {
                return;
            }

            if (Files.isDirectory(paths.training())) {
                if (isRunning(paths.training())) {
                    return;
                }

                // the training exited or timed out without creating the cache, it would most likely fail again
                Path trainingLog = paths.training().resolve(TRAINING_LOG);
                String output = Files.isRegularFile(trainingLog) ? Files.readString(trainingLog, StandardCharsets.UTF_8) : "";
                Files.writeString(paths.failed(), output, StandardCharsets.UTF_8);
                fileService.delete(paths.training());
                log.debug("Training the AOT cache {} failed, see {}", paths.cache(), paths.failed());
                return;
            }

            Files.createDirectories(paths.training());
            List<String> jvmArgs = new ArrayList<>();
            jvmArgs.add("-XX:AOTCacheOutput=" + paths.training().resolve("cache.aot"));
            jvmArgs.add(QUIET);
            Process process = new ProcessBuilder(command.apply(jvmArgs))
                .redirectErrorStream(true)
                .redirectOutput(paths.training().resolve(TRAINING_LOG).toFile())
                .start();
            process.getOutputStream().close();
            Files.writeString(paths.training().resolve(TRAINING_PID), String.valueOf(process.pid()), StandardCharsets.UTF_8);
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
        return new Paths(directory.resolve(key + ".aot"), directory.resolve(key + ".training"), directory.resolve(key + ".failed"));
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
        boolean timedOut = !started.toInstant().isAfter(Instant.now().minus(TRAINING_TIMEOUT));
        Path pidFile = training.resolve(TRAINING_PID);
        if (!Files.isRegularFile(pidFile)) {
            // the process has just been started and its pid not written yet
            return !timedOut;
        }

        Optional<ProcessHandle> process;
        try {
            process = ProcessHandle.of(Long.parseLong(Files.readString(pidFile, StandardCharsets.UTF_8).trim()));
        } catch (NumberFormatException e) {
            return false;
        }

        if (timedOut) {
            process.ifPresent(ProcessHandle::destroy);
            return false;
        }

        return process.map(ProcessHandle::isAlive).orElse(false);
    }

    private record Paths(Path cache, Path training, Path failed) {

    }

}
