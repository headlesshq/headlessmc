package io.github.headlesshq.headlessmc.launcher.process;

import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class McProcessTest {
    // TODO: does this test work everywhere?
    @Test
    public void waitsForRawProcess() throws IOException {
        Process process = new ProcessBuilder("sh", "-c", "exit 3").start();
        McProcess serverProcess = new McProcess("test", Optional.empty(), Optional.of(process));

        assertEquals(3, serverProcess.waitFor(ProcessHandler.defaultHandler()));
    }

    @Test
    public void waitForWithoutProcessReturnsZero() {
        McProcess serverProcess = new McProcess("test", Optional.empty(), Optional.empty());
        assertEquals(0, serverProcess.waitFor(ProcessHandler.defaultHandler()));
    }

}
