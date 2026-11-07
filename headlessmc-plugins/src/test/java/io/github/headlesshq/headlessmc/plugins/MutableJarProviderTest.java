package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MutableJarProviderTest {
    @TempDir
    Path root;

    @Test
    void testExtractConfiguredZip() throws Exception {
        Path zip = root.resolve("headlessmc-mutable.zip");
        PluginScannerTest.jar(zip, Map.of(
            "quarkus-app/quarkus-run.jar", "run",
            "quarkus-app/lib/deployment/appmodel.dat", "model"
        ));

        MutableJarProvider provider = provider(zip);
        Path mutableDir = TestFiles.appFiles(root).getCacheDir().resolve("plugins").resolve("mutable");
        List<Path> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            List<Future<Path>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(provider::get));
            }

            for (Future<Path> future : futures) {
                results.add(future.get());
            }
        }

        Path quarkusApp = results.getFirst();
        assertTrue(results.stream().allMatch(quarkusApp::equals), results.toString());
        assertEquals("run", Files.readString(quarkusApp.resolve("quarkus-run.jar")));
        assertTrue(Files.isRegularFile(quarkusApp.getParent().resolve(".complete")));
        // the zip has been configured by the user and must not be deleted
        assertTrue(Files.isRegularFile(zip));
        try (Stream<Path> files = Files.list(mutableDir)) {
            String sha256 = quarkusApp.getParent().getFileName().toString();
            assertFalse(files.anyMatch(path -> path.getFileName().toString().equals(sha256 + ".tmp")));
        }
    }

    @Test
    void testZipWithoutDeploymentDependencies() throws Exception {
        Path zip = root.resolve("not-mutable.zip");
        PluginScannerTest.jar(zip, Map.of("quarkus-app/quarkus-run.jar", "run"));
        MutableJarProvider provider = provider(zip);
        assertThrows(HeadlessMcException.class, provider::get);
        Path mutableDir = TestFiles.appFiles(root).getCacheDir().resolve("plugins").resolve("mutable");
        try (Stream<Path> files = Files.list(mutableDir)) {
            // nothing that looks complete
            assertTrue(files.noneMatch(path -> Files.exists(path.resolve(".complete"))));
        }
    }

    @SuppressWarnings("unchecked")
    private MutableJarProvider provider(Path mutableJar) {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.mutableJar()).thenReturn(Optional.of(mutableJar.toString()));
        Holder<PluginConfig> holder = () -> config;
        return new MutableJarProvider(holder, Mockito.mock(DownloadService.class),
            new DefaultFileService(new DefaultFileSystemProvider()), new HashServiceImpl(providers), TestFiles.appFiles(root));
    }

}
