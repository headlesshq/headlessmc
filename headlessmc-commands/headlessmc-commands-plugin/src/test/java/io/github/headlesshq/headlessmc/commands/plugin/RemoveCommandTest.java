package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import io.github.headlesshq.headlessmc.plugins.PluginManifest;
import io.github.headlesshq.headlessmc.plugins.PluginScanner;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RemoveCommandTest {
    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();

    @Test
    void testRemoveById() throws IOException {
        AppFiles appFiles = TestFiles.appFiles(root);
        Path jar = plugin(appFiles, "example-1.0.0.jar", "example");
        Path other = plugin(appFiles, "other-1.0.0.jar", "other");

        RemoveCommand command = command(appFiles);
        command.setId("example");
        command.run();

        assertFalse(Files.exists(jar));
        assertTrue(Files.exists(other));
        assertTrue(console.output().contains("Removed example-1.0.0.jar."), console.output());
    }

    @Test
    void testRemoveByFileName() throws IOException {
        AppFiles appFiles = TestFiles.appFiles(root);
        Path jar = plugin(appFiles, "example-1.0.0.jar", "example");

        RemoveCommand command = command(appFiles);
        command.setId("example-1.0.0.jar");
        command.run();
        assertFalse(Files.exists(jar));
    }

    @Test
    void testNotInstalled() throws IOException {
        AppFiles appFiles = TestFiles.appFiles(root);
        plugin(appFiles, "example-1.0.0.jar", "example");

        RemoveCommand command = command(appFiles);
        command.setId("missing");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    private Path plugin(AppFiles appFiles, String fileName, String id) throws IOException {
        Files.createDirectories(appFiles.getPluginDir());
        Path jar = appFiles.getPluginDir().resolve(fileName);
        try (OutputStream outputStream = Files.newOutputStream(jar);
             ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            zip.putNextEntry(new ZipEntry(PluginManifest.PATH));
            zip.write(("{\"schemaVersion\": 1, \"id\": \"" + id + "\", \"version\": \"1.0.0\","
                + " \"name\": \"Example\", \"headlessmc\": \"*\"}").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return jar;
    }

    @SuppressWarnings("unchecked")
    private RemoveCommand command(AppFiles appFiles) {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        PluginScanner scanner = new PluginScanner(new HashServiceImpl(providers), new DefaultJacksonJsonService());
        return new RemoveCommand(scanner, appFiles, console);
    }

}
