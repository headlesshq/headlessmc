package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.headlesshq.headlessmc.commands.plugin.PluginIndexFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class SearchCommandTest {
    private static final String INDEX = """
        {
          "schemaVersion": 1,
          "unknownField": {"ignored": true},
          "plugins": [
            {
              "id": "quilt", "name": "Quilt", "authors": ["headlesshq"],
              "description": "Adds the Quilt mod loader as a platform, with a description too long for the table.",
              "downloads": [
                %s, %s, %s
              ]
            },
            {
              "id": "figlet", "description": "Prints ASCII art.", "authors": ["Someone Else"],
              "downloads": [%s]
            },
            {"id": "minimal"},
            {"id": "../evil", "downloads": []}
          ]
        }
        """.formatted(
        release("1.0.0", "[2.0,3)", "https://example.com/quilt-1.0.0.jar", "0".repeat(64), 1),
        release("1.2.0", "[3.0,4)", "https://example.com/quilt-1.2.0.jar", "0".repeat(64), 1),
        release("2.0.0", "[4.0,5)", "https://example.com/quilt-2.0.0.jar", "0".repeat(64), 1),
        release("0.1", "[4.0,5)", "https://example.com/figlet-0.1.jar", "0".repeat(64), 1)
    );

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final PluginIndexFixture fixture = new PluginIndexFixture().index(INDEX);

    @Test
    void testListAll() throws IOException {
        AppFiles appFiles = TestFiles.appFiles(root);
        Files.createDirectories(appFiles.getPluginDir());
        Files.write(appFiles.getPluginDir().resolve("quilt-old.jar"), pluginJar("quilt", "0.9.0"));

        SearchCommand command = command();
        command.run();
        String output = console.output();
        assertTrue(output.contains("quilt"), output);
        assertTrue(output.contains("figlet"), output);
        assertTrue(output.contains("minimal"), output);
        assertFalse(output.contains("evil"), output);
        // newest compatible version, not the newest version
        assertTrue(output.contains("1.2.0"), output);
        assertFalse(output.contains("2.0.0"), output);
        // installed version
        assertTrue(output.contains("0.9.0"), output);
        assertTrue(output.contains("Adds the Quilt mod loader as a platform, with a descripti..."), output);
        assertTrue(output.contains("plugin download"), output);

        command.setVerbose(true);
        command.run();
        assertTrue(console.output().contains("with a description too long for the table."));
    }

    @Test
    void testFilter() {
        SearchCommand command = command();
        command.setQuery(List.of("ASCII", "else"));
        command.run();
        String output = console.output();
        assertTrue(output.contains("figlet"), output);
        assertFalse(output.contains("quilt"), output);
        // no compatible version
        assertTrue(output.lines().anyMatch(line -> line.contains("figlet") && line.contains(" - ")), output);

        command.setQuery(List.of("mod", "headlesshq"));
        console.lines().clear();
        command.run();
        output = console.output();
        assertTrue(output.contains("quilt"), output);
        assertFalse(output.contains("figlet"), output);
    }

    @Test
    void testNoMatches() {
        SearchCommand command = command();
        command.setQuery(List.of("quilt", "ascii"));
        command.run();
        assertTrue(console.output().contains("No plugins matching 'quilt ascii' found"), console.output());
    }

    @Test
    void testUnsupportedSchemaVersion() {
        fixture.index("{\"schemaVersion\": 2, \"plugins\": []}");
        HeadlessMcException e = assertThrows(HeadlessMcException.class, () -> command().run());
        assertTrue(e.getMessage().contains("schemaVersion 2"), e.getMessage());
    }

    @Test
    void testIndexNotAvailable() {
        fixture.downloadService.register(INDEX_URL, 404, "not found".getBytes());
        HeadlessMcException e = assertThrows(HeadlessMcException.class, () -> command().run());
        assertTrue(e.getMessage().contains("Failed to load the plugin index"), e.getMessage());
    }

    private SearchCommand command() {
        return new SearchCommand(fixture.indexService, fixture.scanner, new SimpleTableProvider(),
            TestFiles.appFiles(root), console);
    }

}
