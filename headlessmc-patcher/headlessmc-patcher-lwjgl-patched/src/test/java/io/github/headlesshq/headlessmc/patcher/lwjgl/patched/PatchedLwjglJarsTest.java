package io.github.headlesshq.headlessmc.patcher.lwjgl.patched;

import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PatchedLwjglJarsTest {
    private static final Path LIBRARIES = Path.of("home", ".minecraft", "libraries");

    private final JsonService jsonService = new DefaultJacksonJsonService();

    @Test
    public void theLockContainsEveryPatchedJar() {
        PatchedLwjglLock lock = PatchedLwjglJars.loadLock(jsonService);
        for (String jar : new String[]{"headlessmc-lwjgl-api.jar", "lwjgl3/lwjgl.jar", "lwjgl3/lwjgl-glfw.jar", "lwjgl3/lwjgl-opengl.jar",
            "lwjgl3/lwjgl-openal.jar", "lwjgl3/lwjgl-stb.jar", "lwjgl3/lwjgl-sdl.jar", "lwjgl2/lwjgl.jar", "lwjgl2/lwjgl_util.jar"}) {
            PatchedLwjglJar locked = lock.jars().get(jar);
            assertNotNull(locked, jar);
            assertTrue(locked.fileName().endsWith("-" + lock.version() + ".jar"), locked.fileName());
            assertEquals("https://github.com/headlesshq/headlessmc-lwjgl/releases/download/" + lock.version() + "/" + locked.fileName(),
                         locked.url());
            assertEquals(64, locked.sha256().length());
            assertTrue(locked.size() > 0);
        }
    }

    @Test
    public void everyReplacedLibraryIsLocked() {
        PatchedLwjglLock lock = PatchedLwjglJars.loadLock(jsonService);
        for (String module : new String[]{"lwjgl", "lwjgl-glfw", "lwjgl-jemalloc", "lwjgl-openal", "lwjgl-opengl", "lwjgl-stb",
            "lwjgl-tinyfd", "lwjgl-freetype", "lwjgl-sdl", "lwjgl-shaderc", "lwjgl-spvc", "lwjgl-vma", "lwjgl-vulkan"}) {
            String jar = PatchedLwjglPatcher.patchedJar(LIBRARIES.resolve("org/lwjgl/" + module + "/3.3.3/" + module + "-3.3.3.jar"));
            assertTrue(lock.jars().containsKey(jar), jar);
        }
    }

    @Test
    public void jarsAreDownloadedOnceAndVerified(@TempDir Path root) throws IOException, NoSuchAlgorithmException {
        byte[] bytes = "patched lwjgl".getBytes(StandardCharsets.UTF_8);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        PatchedLwjglJar glfw = new PatchedLwjglJar("headlessmc-lwjgl3-glfw-1.2.3.jar", bytes.length, sha256,
                                                   "https://example.com/headlessmc-lwjgl3-glfw-1.2.3.jar");
        MockDownloadService downloads = new MockDownloadService().register(glfw.getURL(), bytes);
        PatchedLwjglJars jars = new PatchedLwjglJars(downloads, TestFiles.appFiles(root),
                                                     new PatchedLwjglLock("1.2.3", Map.of("lwjgl3/lwjgl-glfw.jar", glfw)));

        assertTrue(jars.exists("lwjgl3/lwjgl-glfw.jar"));
        assertFalse(jars.exists("lwjgl3/lwjgl-opengl.jar"));
        Path path = jars.get("lwjgl3/lwjgl-glfw.jar");
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertEquals(path, jars.get("lwjgl3/lwjgl-glfw.jar"));
        assertEquals(1, downloads.getRequestedUris().size());
    }

    @Test
    public void aWrongChecksumFailsTheDownload(@TempDir Path root) {
        byte[] bytes = "patched lwjgl".getBytes(StandardCharsets.UTF_8);
        PatchedLwjglJar glfw = new PatchedLwjglJar("headlessmc-lwjgl3-glfw-1.2.3.jar", bytes.length, "0".repeat(64),
                                                   "https://example.com/headlessmc-lwjgl3-glfw-1.2.3.jar");
        MockDownloadService downloads = new MockDownloadService().register(glfw.getURL(), bytes);
        PatchedLwjglJars jars = new PatchedLwjglJars(downloads, TestFiles.appFiles(root),
                                                     new PatchedLwjglLock("1.2.3", Map.of("lwjgl3/lwjgl-glfw.jar", glfw)));

        assertThrows(Exception.class, () -> jars.get("lwjgl3/lwjgl-glfw.jar"));
    }

    @Test
    public void theVersionChangesWithThePatchedJars(@TempDir Path root) {
        PatchedLwjglJar glfw = new PatchedLwjglJar("headlessmc-lwjgl3-glfw-1.2.3.jar", 1, "a".repeat(64), "https://example.com/a.jar");
        PatchedLwjglJar rebuilt = new PatchedLwjglJar("headlessmc-lwjgl3-glfw-1.2.4.jar", 1, "b".repeat(64), "https://example.com/b.jar");
        long version = jars(root, glfw).version();

        assertEquals(version, jars(root, glfw).version());
        assertNotEquals(version, jars(root, rebuilt).version());
    }

    @Test
    public void theVersionOfALocalBuildChangesWithItsJars(@TempDir Path root, @TempDir Path directory) throws IOException {
        PatchedLwjglJar glfw = new PatchedLwjglJar("headlessmc-lwjgl3-glfw-1.2.3.jar", 1, "a".repeat(64), "https://example.com/a.jar");
        Path jar = directory.resolve("lwjgl3/lwjgl-glfw.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "build 1");
        String before = System.getProperty(PatchedLwjglJars.DIRECTORY_PROPERTY);
        System.setProperty(PatchedLwjglJars.DIRECTORY_PROPERTY, directory.toString());
        try {
            long version = jars(root, glfw).version();
            Files.writeString(jar, "second build");
            assertNotEquals(version, jars(root, glfw).version());
        } finally {
            if (before == null) {
                System.clearProperty(PatchedLwjglJars.DIRECTORY_PROPERTY);
            } else {
                System.setProperty(PatchedLwjglJars.DIRECTORY_PROPERTY, before);
            }
        }
    }

    private static PatchedLwjglJars jars(Path root, PatchedLwjglJar glfw) {
        return new PatchedLwjglJars(new MockDownloadService(), TestFiles.appFiles(root),
                                    new PatchedLwjglLock("1.2.3", Map.of("lwjgl3/lwjgl-glfw.jar", glfw)));
    }

}
