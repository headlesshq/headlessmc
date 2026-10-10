package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.PatchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractClassPatcherTest {
    private static final class MarkerPatcher extends AbstractClassPatcher {
        @Override
        public void patch(ClassNode classNode) {
            classNode.interfaces.add("test/Marker");
        }

        @Override
        public boolean skip(String classFileName) {
            return classFileName.endsWith("Skipped.class");
        }

        @Override
        protected boolean shouldPatch(Path library) {
            return library.getFileName().toString().startsWith("patch-me");
        }

        @Override
        public String name() {
            return "marker";
        }

        @Override
        public long version() {
            return 1L;
        }
    }

    @Test
    public void patchesMatchingClassEntries(@TempDir Path root) throws IOException {
        Path jar = TestClasses.writeJar(root.resolve("patch-me.jar"), Map.of(
            "test/A.class", TestClasses.classBytes("test/A", "java/lang/Object"),
            "test/Skipped.class", TestClasses.classBytes("test/Skipped", "java/lang/Object"),
            "module-info.class", TestClasses.classBytes("module-info", "java/lang/Object"),
            "resource.txt", "resource".getBytes(StandardCharsets.UTF_8)
        ));
        PatchResult patchResult = new PatchResult(new LinkedHashSet<>(List.of(jar)), new LinkedHashSet<>());
        FakePatchContext context = new FakePatchContext(root.resolve("base"), patchResult, List.of());
        MarkerPatcher patcher = new MarkerPatcher();

        patcher.patch(context);

        Path patched = root.resolve("base").resolve("marker").resolve("patch-me.jar");
        assertEquals(new LinkedHashSet<>(List.of(patched)), context.getCurrentPatchResult().files());
        try (JarFile result = new JarFile(patched.toFile())) {
            assertNull(result.getEntry("test/Skipped.class"), "skipped entries must be excluded");
            assertNotNull(result.getEntry("module-info.class"));
            assertNotNull(result.getEntry("resource.txt"));

            try (InputStream in = result.getInputStream(result.getEntry("test/A.class"))) {
                ClassNode node = new ClassNode();
                new ClassReader(in).accept(node, 0);
                assertTrue(node.interfaces.contains("test/Marker"), "class entry should be patched");
            }

            try (InputStream in = result.getInputStream(result.getEntry("module-info.class"))) {
                ClassNode node = new ClassNode();
                new ClassReader(in).accept(node, 0);
                assertTrue(node.interfaces == null || node.interfaces.isEmpty(),
                    "module-info must not be patched");
            }
        }
    }

    @Test
    public void ignoresLibrariesThatShouldNotBePatched(@TempDir Path root) throws IOException {
        Path jar = TestClasses.writeJar(root.resolve("leave-me.jar"), Map.of(
            "test/A.class", TestClasses.classBytes("test/A", "java/lang/Object")
        ));
        PatchResult patchResult = new PatchResult(new LinkedHashSet<>(List.of(jar)), new LinkedHashSet<>());
        FakePatchContext context = new FakePatchContext(root.resolve("base"), patchResult, List.of());

        new MarkerPatcher().patch(context);

        assertEquals(patchResult, context.getCurrentPatchResult());
    }

    @Test
    public void matchesOnlyRegularClassFiles() {
        MarkerPatcher patcher = new MarkerPatcher();
        assertTrue(patcher.matches("test/A.class"));
        assertFalse(patcher.matches("module-info.class"));
        assertFalse(patcher.matches("resource.txt"));
    }

    @Test
    public void convertsClassFileNamesToClassNames() {
        MarkerPatcher patcher = new MarkerPatcher();
        assertEquals("test/A", patcher.classFileAsClassName("test/A.class"));
        assertEquals("test/A", patcher.classFileAsClassName("test/A"));
    }

}
