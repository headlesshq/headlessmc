package io.github.headlesshq.headlessmc.plugins.augmenter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

class NativeAugmentTaskTest {
    @Test
    void testAddProviders(@TempDir Path dir) throws IOException {
        Path providers = Files.createDirectories(dir.resolve("providers"));
        Files.writeString(providers.resolve("plugin-b.jar"), "b");
        Files.writeString(providers.resolve("plugin-a.jar"), "a");
        Files.writeString(providers.resolve(".keep"), "");
        Path sources = Files.createDirectories(dir.resolve("native-sources"));
        Path runner = sources.resolve("app-runner.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, "lib/a.jar lib/b.jar");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "io.quarkus.runner.GeneratedMain");
        try (OutputStream outputStream = Files.newOutputStream(runner); JarOutputStream jar = new JarOutputStream(outputStream, manifest)) {
            jar.putNextEntry(new ZipEntry("io/quarkus/runner/GeneratedMain.class"));
            jar.write("class".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        NativeAugmentTask.addProviders(providers, sources);

        assertEquals("a", Files.readString(sources.resolve("lib/providers/plugin-a.jar")));
        assertEquals("b", Files.readString(sources.resolve("lib/providers/plugin-b.jar")));
        assertFalse(Files.exists(sources.resolve("lib/providers/.keep")));
        try (JarFile jarFile = new JarFile(runner.toFile())) {
            Attributes attributes = jarFile.getManifest().getMainAttributes();
            assertEquals("lib/a.jar lib/b.jar lib/providers/plugin-a.jar lib/providers/plugin-b.jar",
                attributes.getValue(Attributes.Name.CLASS_PATH));
            assertEquals("io.quarkus.runner.GeneratedMain", attributes.getValue(Attributes.Name.MAIN_CLASS));
            assertNotNull(jarFile.getEntry("io/quarkus/runner/GeneratedMain.class"));
        }
    }

    @Test
    void testNoProviders(@TempDir Path dir) throws IOException {
        NativeAugmentTask.addProviders(dir.resolve("missing"), dir);
        assertFalse(Files.exists(dir.resolve("lib")));
    }

}
