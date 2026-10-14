package io.github.headlesshq.headlessmc.patcher.log4j;

import io.github.headlesshq.headlessmc.patcher.PatchResult;
import io.github.headlesshq.headlessmc.patcher.asm.FakePatchContext;
import io.github.headlesshq.headlessmc.patcher.asm.TestClasses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class Log4JPatcherTest {
    private static final String JNDI_LOOKUP = "org/apache/logging/log4j/core/lookup/JndiLookup";

    @TempDir
    Path root;

    private Path log4jCore() throws IOException {
        return TestClasses.writeJar(
            root.resolve("libraries/org/apache/logging/log4j/log4j-core/2.8.1/log4j-core-2.8.1.jar"),
            Map.of(
                JNDI_LOOKUP + ".class", TestClasses.classBytes(JNDI_LOOKUP, "java/lang/Object"),
                "org/apache/logging/log4j/core/lookup/MainMapLookup.class",
                TestClasses.classBytes("org/apache/logging/log4j/core/lookup/MainMapLookup", "java/lang/Object"),
                "META-INF/services/test", "service".getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    @Test
    void jndiLookupIsRemovedFromLog4J() throws IOException {
        Path log4j = log4jCore();
        Path other = TestClasses.writeJar(root.resolve("libraries/other.jar"), Map.of(
            JNDI_LOOKUP + ".class", TestClasses.classBytes(JNDI_LOOKUP, "java/lang/Object")
        ));
        PatchResult patchResult = new PatchResult(new LinkedHashSet<>(List.of(log4j, other)), new LinkedHashSet<>());
        FakePatchContext context = new FakePatchContext(root.resolve("patched"), patchResult, List.of());

        new Log4JPatcher().patch(context);

        Path patched = root.resolve("patched/log4j/log4j-core-2.8.1.jar");
        assertEquals(List.of(other, patched), List.copyOf(context.getCurrentPatchResult().files()));
        try (JarFile result = new JarFile(patched.toFile())) {
            assertNull(result.getEntry(JNDI_LOOKUP + ".class"));
            assertNotNull(result.getEntry("org/apache/logging/log4j/core/lookup/MainMapLookup.class"));
            assertNotNull(result.getEntry("META-INF/services/test"));
        }
    }

    @Test
    void log4JWithoutJndiLookupIsLeftAlone() throws IOException {
        Path log4j = TestClasses.writeJar(root.resolve("libraries/log4j-api-2.8.1.jar"), Map.of(
            "org/apache/logging/log4j/Logger.class",
            TestClasses.classBytes("org/apache/logging/log4j/Logger", "java/lang/Object")
        ));
        PatchResult patchResult = new PatchResult(new LinkedHashSet<>(List.of(log4j)), new LinkedHashSet<>());
        FakePatchContext context = new FakePatchContext(root.resolve("patched"), patchResult, List.of());

        new Log4JPatcher().patch(context);

        assertEquals(patchResult, context.getCurrentPatchResult());
    }

    @Test
    void patcherCanBeReused() throws IOException {
        Log4JPatcher patcher = new Log4JPatcher();
        Path log4j = log4jCore();
        FakePatchContext first = new FakePatchContext(
            root.resolve("first"), new PatchResult(new LinkedHashSet<>(List.of(log4j)), new LinkedHashSet<>()), List.of()
        );
        FakePatchContext second = new FakePatchContext(
            root.resolve("second"), new PatchResult(new LinkedHashSet<>(List.of(log4j)), new LinkedHashSet<>()), List.of()
        );

        patcher.patch(first);
        patcher.patch(second);

        assertEquals(List.of(root.resolve("first/log4j/log4j-core-2.8.1.jar")), List.copyOf(first.getCurrentPatchResult().files()));
        assertEquals(List.of(root.resolve("second/log4j/log4j-core-2.8.1.jar")), List.copyOf(second.getCurrentPatchResult().files()));
    }

    @Test
    void onlyJndiLookupIsSkipped() {
        Log4JPatcher patcher = new Log4JPatcher();

        assertTrue(patcher.skip(JNDI_LOOKUP + ".class"));
        assertFalse(patcher.skip("org/apache/logging/log4j/core/lookup/JndiLookup$1.class"));
        assertFalse(patcher.skip("org/apache/logging/log4j/core/lookup/MainMapLookup.class"));
        assertFalse(patcher.skip("JndiLookup.class"));
    }

    @Test
    void noClassIsTransformed() {
        Log4JPatcher patcher = new Log4JPatcher();
        ClassNode node = new ClassNode();
        node.name = JNDI_LOOKUP;

        patcher.patch(node);

        assertFalse(patcher.matches(JNDI_LOOKUP + ".class"));
        assertFalse(patcher.matches("org/apache/logging/log4j/Logger.class"));
        assertEquals(JNDI_LOOKUP, node.name);
    }

    @Test
    void onlyLog4JLibrariesArePatched() {
        Log4JPatcher patcher = new Log4JPatcher();

        assertTrue(patcher.shouldPatch(Path.of("libraries/org/apache/logging/log4j/log4j-core/2.8.1/log4j-core-2.8.1.jar")));
        assertTrue(patcher.shouldPatch(Path.of("libraries/LOG4J-core.jar")));
        assertFalse(patcher.shouldPatch(Path.of("libraries/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar")));
    }

    @Test
    void nameAndVersion() {
        Log4JPatcher patcher = new Log4JPatcher();

        assertEquals("log4j", patcher.name());
        assertEquals(0L, patcher.version());
    }

}
