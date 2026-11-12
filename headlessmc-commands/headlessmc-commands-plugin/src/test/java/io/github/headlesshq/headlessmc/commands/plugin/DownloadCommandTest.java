package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.TestFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static io.github.headlesshq.headlessmc.commands.plugin.PluginIndexFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class DownloadCommandTest {
    private static final URI OLD_URL = URI.create("https://example.com/example-1.0.0.jar");
    private static final URI NEW_URL = URI.create("https://example.com/example-1.1.0.jar");
    private static final URI INCOMPATIBLE_URL = URI.create("https://example.com/example-2.0.0.jar");
    private static final URI WRONG_URL = URI.create("https://example.com/wrong.jar");
    private static final URI OTHER_URL = URI.create("https://example.com/other.jar");
    private static final byte[] OLD = pluginJar("example", "1.0.0");
    private static final byte[] NEW = pluginJar("example", "1.1.0");
    private static final byte[] INCOMPATIBLE = pluginJar("example", "2.0.0");
    private static final byte[] OTHER = pluginJar("other", "1.0.0");
    private static final URI INCOMPATIBLE_PLUGIN_URL = URI.create("https://example.com/incompatible-2.0.0.jar");
    private static final byte[] INCOMPATIBLE_PLUGIN = pluginJar("incompatible", "2.0.0");

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final PluginIndexFixture fixture = new PluginIndexFixture().index("""
        {
          "schemaVersion": 1,
          "plugins": [
            {"id": "example", "name": "Example", "downloads": [%s, %s, %s]},
            {"id": "wrong-hash", "downloads": [%s]},
            {"id": "impostor", "downloads": [%s]},
            {"id": "incompatible", "downloads": [%s]}
          ]
        }
        """.formatted(
        release("1.0.0", "[3.0,4)", OLD_URL.toString(), OLD),
        release("2.0.0", "[4.0,5)", INCOMPATIBLE_URL.toString(), INCOMPATIBLE),
        release("1.1.0", "[3.0,4)", NEW_URL.toString(), NEW),
        release("1.0.0", "*", WRONG_URL.toString(), "a".repeat(64), OLD.length),
        release("1.0.0", "*", OTHER_URL.toString(), OTHER),
        release("2.0.0", "[4.0,5)", INCOMPATIBLE_PLUGIN_URL.toString(), INCOMPATIBLE_PLUGIN)
    ));

    {
        fixture.downloadService
            .register(OLD_URL, OLD)
            .register(NEW_URL, NEW)
            .register(INCOMPATIBLE_URL, INCOMPATIBLE)
            .register(WRONG_URL, OLD)
            .register(OTHER_URL, OTHER)
            .register(INCOMPATIBLE_PLUGIN_URL, INCOMPATIBLE_PLUGIN);
    }

    @Test
    void testDownloadNewestCompatible() throws IOException {
        command("example", null).run();
        Path jar = pluginDir().resolve("example-1.1.0.jar");
        assertArrayEquals(NEW, Files.readAllBytes(jar));
        assertEquals(List.of(jar), files());
        assertTrue(console.output().contains("loaded the next time HeadlessMc starts"), console.output());
        assertTrue(fixture.downloadService.wasRequested(NEW_URL));
    }

    @Test
    void testUpgradeReplacesOldVersion() throws IOException {
        Files.createDirectories(pluginDir());
        // the old version under a different file name, e.g. installed manually
        Files.write(pluginDir().resolve("my-example.jar"), OLD);
        Files.write(pluginDir().resolve("other.jar"), OTHER);

        command("example", null).run();
        assertEquals(List.of(pluginDir().resolve("example-1.1.0.jar"), pluginDir().resolve("other.jar")), files());
        assertTrue(console.output().contains("Removed my-example.jar"), console.output());

        // downgrade to an explicitly given version
        command("example", "1.0.0").run();
        assertEquals(List.of(pluginDir().resolve("example-1.0.0.jar"), pluginDir().resolve("other.jar")), files());
    }

    @Test
    void testAlreadyInstalled() throws IOException {
        command("example", null).run();
        fixture.downloadService.getRequestedUris().clear();
        command("example", "1.1.0").run();
        assertFalse(fixture.downloadService.wasRequested(NEW_URL));
        assertTrue(console.output().contains("already installed"), console.output());
        assertEquals(List.of(pluginDir().resolve("example-1.1.0.jar")), files());
    }

    @Test
    void testWrongHashLeavesNothingBehind() throws IOException {
        assertThrows(HeadlessMcException.class, () -> command("wrong-hash", null).run());
        assertEquals(List.of(), files());
    }

    @Test
    void testWrongHashKeepsInstalledVersion() throws IOException {
        Files.createDirectories(pluginDir());
        Files.write(pluginDir().resolve("wrong-hash-1.0.0.jar"), NEW);
        assertThrows(HeadlessMcException.class, () -> command("wrong-hash", null).run());
        assertEquals(List.of(pluginDir().resolve("wrong-hash-1.0.0.jar")), files());
        assertArrayEquals(NEW, Files.readAllBytes(pluginDir().resolve("wrong-hash-1.0.0.jar")));
    }

    @Test
    void testJarWithOtherIdIsRejected() throws IOException {
        HeadlessMcException e = assertThrows(HeadlessMcException.class, () -> command("impostor", null).run());
        assertTrue(e.getMessage().contains("is not the plugin impostor"), e.getMessage());
        assertEquals(List.of(), files());
    }

    @Test
    void testIncompatible() throws IOException {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> command("incompatible", null).run());
        assertTrue(e.getMessage().contains("No version of incompatible is compatible with HeadlessMc 3.0.0"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> command("example", "2.0.0").run());
        assertTrue(e.getMessage().contains("--force"), e.getMessage());
        assertEquals(List.of(), files());

        DownloadCommand command = command("incompatible", null);
        command.setForce(true);
        command.run();
        assertEquals(List.of(pluginDir().resolve("incompatible-2.0.0.jar")), files());
        assertTrue(console.output().contains("Warning: incompatible 2.0.0 is not compatible"), console.output());
    }

    @Test
    void testUnknownPluginAndVersion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> command("unknown", null).run());
        assertTrue(e.getMessage().contains("Unknown plugin 'unknown'"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> command("example", "9.9.9").run());
        assertTrue(e.getMessage().contains("has no version 9.9.9"), e.getMessage());
        assertTrue(e.getMessage().contains("1.1.0"), e.getMessage());
    }

    @Test
    void testUnsafeIdAndVersionAreRejected() throws IOException {
        for (String id : List.of("../example", "Example", "example/../x", "")) {
            assertThrows(IllegalArgumentException.class, () -> command(id, null).run(), id);
        }

        for (String version : List.of("../1.0.0", "1.0/../..", "1.0\\x")) {
            assertThrows(IllegalArgumentException.class, () -> command("example", version).run(), version);
        }

        assertFalse(fixture.downloadService.wasRequested(INDEX_URL));
        assertEquals(List.of(), files());
    }

    private DownloadCommand command(String id, String version) {
        DownloadCommand command = new DownloadCommand(fixture.indexService, TestFiles.appFiles(root), console);
        command.setId(id);
        command.setVersion(version);
        return command;
    }

    private Path pluginDir() {
        return TestFiles.appFiles(root).getPluginDir();
    }

    private List<Path> files() throws IOException {
        if (!Files.isDirectory(pluginDir())) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(pluginDir())) {
            return files.sorted().toList();
        }
    }

}
