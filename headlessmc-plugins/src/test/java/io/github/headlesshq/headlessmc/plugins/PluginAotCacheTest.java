package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PluginAotCacheTest {
    @TempDir
    Path root;

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testTrainInBackgroundAndUseOnNextStart() throws Exception {
        PluginAotCache cache = cache(Optional.of(true));
        PluginLaunchCache.Launch launch = launch(25);
        assertEquals(List.of(), cache.jvmArgs(launch));

        // stands in for the training JVM: writes the cache and the line the JVM prints when it is done
        cache.train(launch, jvmArgs -> {
            String output = jvmArgs.getFirst().substring("-XX:AOTCacheOutput=".length());
            assertEquals(PluginAotCache.QUIET, jvmArgs.get(1));
            return List.of("sh", "-c", "touch \"$0\" && echo '" + PluginAotCache.COMPLETE + ": '\"$0\"", output);
        });

        Path aot = launch.build().resolve("aot");
        awaitTraining(aot);
        // a second training must not be started while one is running or the cache exists
        cache.train(launch, jvmArgs -> fail("trained twice"));

        List<String> jvmArgs = cache.jvmArgs(launch);
        assertEquals(2, jvmArgs.size());
        assertTrue(jvmArgs.getFirst().startsWith("-XX:AOTCache=") && jvmArgs.getFirst().endsWith("-25.aot"), jvmArgs.toString());
        assertEquals(PluginAotCache.QUIET, jvmArgs.get(1));
        try (Stream<Path> files = Files.list(aot)) {
            // the training directory has been removed
            assertEquals(1, files.count());
        }

        cache.train(launch, jvmArgs1 -> fail("trained although the cache exists"));
    }

    @Test
    void testDisabled() {
        PluginLaunchCache.Launch launch = launch(25);
        cache(Optional.of(false)).train(launch, jvmArgs -> fail("training although disabled"));
        cache(Optional.of(true)).train(launch(21), jvmArgs -> fail("training on Java 21"));
        assertEquals(List.of(), cache(Optional.of(false)).jvmArgs(launch));
    }

    private void awaitTraining(Path aot) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            try (Stream<Path> trainings = Files.list(aot)) {
                Optional<Path> training = trainings.filter(path -> path.toString().endsWith(".training")).findFirst();
                if (training.isPresent()
                    && Files.isRegularFile(training.get().resolve(PluginAotCache.TRAINING_LOG))
                    && Files.readString(training.get().resolve(PluginAotCache.TRAINING_LOG)).contains(PluginAotCache.COMPLETE)) {
                    return;
                }
            }

            Thread.sleep(20L);
        }

        fail("Training did not finish");
    }

    private PluginLaunchCache.Launch launch(int javaVersion) {
        try {
            Path build = Files.createDirectories(root.resolve("build-" + javaVersion));
            Path java = root.resolve("java");
            if (!Files.exists(java)) {
                Files.writeString(java, "");
            }

            return new PluginLaunchCache.Launch(build, root, java, javaVersion);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private PluginAotCache cache(Optional<Boolean> enabled) {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        PluginLaunchCache launchCache = new PluginLaunchCache(new HashServiceImpl(providers), TestFiles.appFiles(root));
        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.aotCache()).thenReturn(enabled);
        Holder<PluginConfig> holder = () -> config;
        return new PluginAotCache(holder, launchCache, new DefaultFileService(new DefaultFileSystemProvider()));
    }

}
