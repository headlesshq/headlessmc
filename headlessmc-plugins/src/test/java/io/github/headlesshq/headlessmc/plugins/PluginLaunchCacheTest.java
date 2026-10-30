package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
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
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PluginLaunchCacheTest {
    @TempDir
    Path root;

    @Test
    void testFingerprintChangesWithMetadata() throws IOException {
        PluginLaunchCache cache = cache();
        Path plugin = Files.writeString(root.resolve("plugin.jar"), "a");
        Files.setLastModifiedTime(plugin, FileTime.fromMillis(1000L));
        String fingerprint = cache.fingerprint(List.of(plugin), null);
        assertEquals(fingerprint, cache.fingerprint(List.of(plugin), null));

        Files.setLastModifiedTime(plugin, FileTime.fromMillis(2000L));
        String touched = cache.fingerprint(List.of(plugin), null);
        assertNotEquals(fingerprint, touched);

        Files.writeString(plugin, "ab");
        Files.setLastModifiedTime(plugin, FileTime.fromMillis(2000L));
        assertNotEquals(touched, cache.fingerprint(List.of(plugin), null));

        Path other = Files.writeString(root.resolve("other.jar"), "b");
        assertNotEquals(cache.fingerprint(List.of(plugin), null), cache.fingerprint(List.of(plugin, other), null));
        assertNotEquals(cache.fingerprint(List.of(plugin), null), cache.fingerprint(List.of(plugin), root.resolve("mutable.zip")));
    }

    @Test
    void testPutAndGet() throws IOException {
        PluginLaunchCache cache = cache();
        AppFiles appFiles = TestFiles.appFiles(root);
        Path build = appFiles.getCacheDir().resolve("plugins").resolve("builds").resolve("abc");
        Files.createDirectories(build.resolve(PluginBuilder.QUARKUS_APP));
        Path runJar = Files.writeString(build.resolve(PluginBuilder.QUARKUS_APP).resolve("quarkus-run.jar"), "");
        Path java = Files.writeString(root.resolve("java"), "");
        Files.setPosixFilePermissions(java, PosixFilePermissions.fromString("rwxr-xr-x"));

        PluginLaunchCache.Launch launch = new PluginLaunchCache.Launch(build, root, java, 25);
        assertEquals(Optional.empty(), cache.get("fingerprint"));
        cache.put("fingerprint", launch);
        assertEquals(Optional.of(launch), cache.get("fingerprint"));
        assertEquals(Optional.empty(), cache.get("other"));

        // the build or the Java might have been deleted in the meantime
        Files.delete(runJar);
        assertEquals(Optional.empty(), cache.get("fingerprint"));
    }

    @SuppressWarnings("unchecked")
    private PluginLaunchCache cache() {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        return new PluginLaunchCache(new HashServiceImpl(providers), TestFiles.appFiles(root));
    }

}
