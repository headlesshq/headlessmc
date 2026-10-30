package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import io.github.headlesshq.headlessmc.plugins.CoreLibraries;
import io.github.headlesshq.headlessmc.plugins.PluginBuilder;
import io.github.headlesshq.headlessmc.plugins.PluginManifest;
import io.github.headlesshq.headlessmc.plugins.PluginNativeCompiler;
import io.github.headlesshq.headlessmc.plugins.PluginResolver;
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
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ListCommandTest {
    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();

    @Test
    void testNoPlugins() {
        command().run();
        assertTrue(console.output().contains("No plugins installed"));
    }

    @Test
    void testListPluginsAndLibraries() throws IOException {
        AppFiles appFiles = TestFiles.appFiles(root);
        Files.createDirectories(appFiles.getPluginDir());
        try (OutputStream outputStream = Files.newOutputStream(appFiles.getPluginDir().resolve("example.jar"));
             ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            zip.putNextEntry(new ZipEntry(PluginManifest.PATH));
            zip.write("""
                {
                  "schemaVersion": 1, "id": "example", "version": "1.2.3", "name": "Example", "headlessmc": "*",
                  "description": "A very long description of the example plugin, which does not fit into the table.",
                  "libraries": [{"group": "com.example", "artifact": "lib", "version": "1.0",
                                 "path": "META-INF/jars/lib-1.0.jar", "sha256": "abc"}]
                }
                """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Files.writeString(appFiles.getPluginDir().resolve("broken.jar"), "not a zip");

        ListCommand command = command();
        command.run();
        String output = console.output();
        assertTrue(output.contains("example"), output);
        assertTrue(output.contains("1.2.3"), output);
        assertTrue(output.contains("not loaded"), output);
        assertTrue(output.contains("error: failed to read"), output);
        assertTrue(output.contains("running without plugins"), output);
        assertTrue(output.contains("A very long description of the example plugin, which does..."), output);

        command.setVerbose(true);
        command.run();
        assertTrue(console.output().contains("which does not fit into the table."));
        command.setVerbose(false);

        command.setLibraries(true);
        command.run();
        assertTrue(console.output().contains("com.example:lib"));
    }

    @SuppressWarnings("unchecked")
    private ListCommand command() {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        PluginScanner scanner = new PluginScanner(new HashServiceImpl(providers), new DefaultJacksonJsonService());
        PluginResolver resolver = new PluginResolver(new CoreLibraries(Map.of()), "3.0.0");
        return new ListCommand(scanner, resolver, Mockito.mock(PluginBuilder.class), Mockito.mock(PluginNativeCompiler.class),
            new SimpleTableProvider(), TestFiles.appFiles(root), console);
    }

}
