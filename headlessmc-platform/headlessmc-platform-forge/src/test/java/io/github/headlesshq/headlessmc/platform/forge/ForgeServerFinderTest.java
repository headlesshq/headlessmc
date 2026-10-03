package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ForgeServerFinderTest {
    @Inject
    @Forge
    ServerFinder finder;

    @Test
    public void testForgeServerFinder() throws IOException {
        assertInstanceOf(ForgeServerFinder.class, finder);
        Path tempDir = Files.createTempDirectory("forge");
        assertThrows(HeadlessMcException.class, () -> finder.findExecutable(tempDir));
        Path file = Files.createFile(tempDir.resolve("forge-server.jar"));
        assertEquals(file, finder.findExecutable(tempDir));
    }

    @Test
    public void testWindowsForgeServerFinder() throws IOException {
        ServerFinder finder = new WindowsForgeServerFinder();
        Path tempDir = Files.createTempDirectory("forge");
        assertThrows(HeadlessMcException.class, () -> finder.findExecutable(tempDir));
        Path file = Files.createFile(tempDir.resolve("forge-server.jar"));
        assertEquals(file, finder.findExecutable(tempDir));
        Files.createFile(tempDir.resolve("run.sh"));
        assertEquals(file, finder.findExecutable(tempDir));
        file = Files.createFile(tempDir.resolve("run.bat"));
        assertEquals(file, finder.findExecutable(tempDir));
    }

    @Test
    public void testLinuxForgeServerFinder() throws IOException {
        ServerFinder finder = new LinuxForgeServerFinder();
        Path tempDir = Files.createTempDirectory("forge");
        assertThrows(HeadlessMcException.class, () -> finder.findExecutable(tempDir));
        Path file = Files.createFile(tempDir.resolve("forge-server.jar"));
        assertEquals(file, finder.findExecutable(tempDir));
        Files.createFile(tempDir.resolve("run.bat"));
        assertEquals(file, finder.findExecutable(tempDir));
        file = Files.createFile(tempDir.resolve("run.sh"));
        assertEquals(file, finder.findExecutable(tempDir));
    }

    private static final class WindowsForgeServerFinder extends ForgeServerFinder {
        public WindowsForgeServerFinder() {
            super(Forge.PLATFORM_NAME, new OS("windows", OS.Type.WINDOWS, "11"));
        }
    }

    private static final class LinuxForgeServerFinder extends ForgeServerFinder {
        public LinuxForgeServerFinder() {
            super(Forge.PLATFORM_NAME, new OS("linux", OS.Type.LINUX, "24"));
        }
    }

}
