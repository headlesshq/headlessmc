package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PluginBuilderTest {
    @TempDir
    Path root;

    @Test
    void testUseTouchesBuild() throws IOException {
        PluginBuilder builder = builder(3);
        Path build = build(builder, "a", 1000L);
        assertEquals(Optional.empty(), builder.use(builder.getBuildsDir().resolve("missing")));
        try (PluginBuilder.Lease ignored = builder.use(build).orElseThrow()) {
            // a build used every day must not look old
            assertTrue(Files.getLastModifiedTime(build).toMillis() > 1000L);
        }
    }

    @Test
    void testPruneKeepsBuildsInUse() throws IOException {
        PluginBuilder builder = builder(1);
        Path current = build(builder, "current", 3000L);
        Path used = build(builder, "used", 1000L);
        Path unused = build(builder, "unused", 2000L);
        try (PluginBuilder.Lease ignored = builder.use(used).orElseThrow()) {
            builder.prune(builder.getBuildsDir(), current);
            assertTrue(Files.exists(current));
            assertTrue(Files.exists(used));
            assertFalse(Files.exists(unused));
        }

        builder.prune(builder.getBuildsDir(), current);
        assertFalse(Files.exists(used));
        try (Stream<Path> files = Files.list(builder.getBuildsDir())) {
            assertEquals(1, files.filter(Files::isDirectory).count());
        }
    }

    private static Path build(PluginBuilder builder, String hash, long lastModified) throws IOException {
        Path build = builder.getBuildsDir().resolve(hash);
        Files.createDirectories(build.resolve(PluginBuilder.QUARKUS_APP));
        Files.writeString(build.resolve(PluginBuilder.QUARKUS_APP).resolve("quarkus-run.jar"), "");
        Files.writeString(build.resolve(PluginBuilder.PLUGINS_FILE), "");
        Files.setLastModifiedTime(build, FileTime.fromMillis(lastModified));
        return build;
    }

    @SuppressWarnings("unchecked")
    private PluginBuilder builder(int keepBuilds) {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.keepBuilds()).thenReturn(keepBuilds);
        Holder<PluginConfig> holder = () -> config;
        return new PluginBuilder(holder, Mockito.mock(JavaLauncherService.class), new DefaultFileService(new DefaultFileSystemProvider()),
            new HashServiceImpl(providers), TestFiles.appFiles(root), Mockito.mock(Console.class));
    }

}
