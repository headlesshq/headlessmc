package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
public class CommandTestServiceTest {
    @TempDir
    Path tempDir;

    @Test
    public void noTestIsActiveByDefault() throws IOException {
        CommandTestService service = service(new Config(false, Optional.empty()));
        assertFalse(service.isTestActive());
        assertNull(service.createCommandTest(FakeProcess.withLines(true), _ -> {}));
    }

    @Test
    public void serverTestIsActive() {
        assertTrue(service(new Config(true, Optional.empty())).isTestActive());
    }

    @Test
    public void fileTestIsActive() {
        assertTrue(service(new Config(false, Optional.of("test.json"))).isTestActive());
    }

    @Test
    public void serverAndFileAreMutuallyExclusive() {
        CommandTestService service = service(new Config(true, Optional.of("test.json")));
        assertThrows(ConfigException.class, service::isTestActive);
        assertThrows(ConfigException.class, () -> service.createCommandTest(FakeProcess.withLines(true), _ -> {}));
    }

    @Test
    public void createsTheBundledServerTest() throws IOException {
        CommandTestService service = service(new Config(true, Optional.empty()));
        FakeProcess process = FakeProcess.withLines(true, "Starting", "Done (2.5s)! For help, type \"help\"");

        try (CommandTest commandTest = service.createCommandTest(process, _ -> {})) {
            assertNotNull(commandTest);
            commandTest.run();
            assertTrue(commandTest.wasSuccessful());
        }

        assertEquals("stop" + System.lineSeparator(), process.getWrittenInput());
    }

    @Test
    public void createsTestFromFile() throws IOException {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, """
            {
              "name": "File Test",
              "implicitWaitForEnd": false,
              "steps": [
                { "type": "CONTAINS", "message": "ready" },
                { "type": "SEND", "message": "quit" },
                { "type": "SUCCESS" }
              ]
            }
            """);

        CommandTestService service = service(new Config(false, Optional.of(file.toString())));
        FakeProcess process = FakeProcess.withLines(false, "booting", "ready");

        try (CommandTest commandTest = service.createCommandTest(process, _ -> {})) {
            assertNotNull(commandTest);
            commandTest.run();
            assertTrue(commandTest.wasSuccessful());
        }

        assertEquals("quit" + System.lineSeparator(), process.getWrittenInput());
    }

    @Test
    public void missingFileThrows() {
        CommandTestService service = service(new Config(false, Optional.of(tempDir.resolve("missing.json").toString())));
        assertThrows(NoSuchFileException.class, () -> service.createCommandTest(FakeProcess.withLines(true), _ -> {}));
    }

    private static CommandTestService service(Config config) {
        return new CommandTestService(() -> config, new DefaultJacksonJsonService());
    }

    private record Config(boolean server, Optional<String> file) implements TestConfig {
        @Override
        public boolean leave() {
            return true;
        }

        @Override
        public boolean noTimeout() {
            return false;
        }
    }

}
