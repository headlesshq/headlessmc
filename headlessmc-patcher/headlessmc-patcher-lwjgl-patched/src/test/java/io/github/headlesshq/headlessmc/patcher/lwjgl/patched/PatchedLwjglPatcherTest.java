package io.github.headlesshq.headlessmc.patcher.lwjgl.patched;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PatchedLwjglPatcherTest {
    private static final Path LIBRARIES = Path.of("home", ".minecraft", "libraries");

    @Test
    public void lwjgl3ModulesAreReplacedByTheirPatchedModule() {
        assertEquals("lwjgl3/lwjgl-glfw.jar",
                     PatchedLwjglPatcher.patchedJar(LIBRARIES.resolve("org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3.jar")));
        assertEquals("lwjgl3/lwjgl.jar",
                     PatchedLwjglPatcher.patchedJar(LIBRARIES.resolve("org/lwjgl/lwjgl/3.1.6/lwjgl-3.1.6.jar")));
    }

    @Test
    public void lwjgl2IsReplacedByThePatchedLwjgl2() {
        assertEquals("lwjgl2/lwjgl.jar", PatchedLwjglPatcher.patchedJar(
            LIBRARIES.resolve("org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar")));
        assertEquals("lwjgl2/lwjgl_util.jar", PatchedLwjglPatcher.patchedJar(
            LIBRARIES.resolve("org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar")));
    }

    @Test
    public void nativesAndOtherLibrariesAreKept() {
        assertNull(PatchedLwjglPatcher.patchedJar(
            LIBRARIES.resolve("org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-linux.jar")));
        assertNull(PatchedLwjglPatcher.patchedJar(
            LIBRARIES.resolve("org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-linux.jar")));
        assertNull(PatchedLwjglPatcher.patchedJar(LIBRARIES.resolve("org/joml/joml/1.10.5/joml-1.10.5.jar")));
        assertNull(PatchedLwjglPatcher.patchedJar(Path.of("lwjgl.jar")));
    }

    @Test
    public void everyPatchedJarIsBundled() {
        for (String jar : new String[]{"headlessmc-lwjgl-api.jar", "headlessmc-macos-menu-agent.jar", "lwjgl3/lwjgl.jar", "lwjgl3/lwjgl-glfw.jar",
            "lwjgl3/lwjgl-opengl.jar", "lwjgl3/lwjgl-sdl.jar", "lwjgl2/lwjgl.jar", "lwjgl2/lwjgl_util.jar"}) {
            assertNotNull(getClass().getClassLoader().getResource(PatchedLwjglPatcher.RESOURCES + jar), jar);
        }
    }

    @Test
    public void moduleDescriptorsAreDetected() {
        assertTrue(PatchedLwjglPatcher.isModuleDescriptor("module-info.class"));
        assertTrue(PatchedLwjglPatcher.isModuleDescriptor("META-INF/versions/9/module-info.class"));
        assertFalse(PatchedLwjglPatcher.isModuleDescriptor("io/github/headlesshq/headlessmc/lwjgl/api/Keys.class"));
    }

}
