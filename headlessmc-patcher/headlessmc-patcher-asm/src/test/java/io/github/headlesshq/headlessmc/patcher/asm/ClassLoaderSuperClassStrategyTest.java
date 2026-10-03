package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.Classpath;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClassLoaderSuperClassStrategyTest {
    private final ClassLoaderSuperClassStrategy strategy = new ClassLoaderSuperClassStrategy();

    private FakePatchContext context(Path root, Classpath classpath) {
        return new FakePatchContext(root.resolve("base"), classpath, List.of());
    }

    @Test
    public void findsCommonJdkSuperClass(@TempDir Path root) throws Exception {
        try (SuperClassResolver resolver = strategy.apply(context(root, emptyClasspath()))) {
            assertEquals("java/lang/Number", resolver.getCommonSuperClass("java/lang/Integer", "java/lang/Long"));
        }
    }

    @Test
    public void returnsAssignableType(@TempDir Path root) throws Exception {
        try (SuperClassResolver resolver = strategy.apply(context(root, emptyClasspath()))) {
            assertEquals("java/lang/Number", resolver.getCommonSuperClass("java/lang/Number", "java/lang/Integer"));
            assertEquals("java/lang/Number", resolver.getCommonSuperClass("java/lang/Integer", "java/lang/Number"));
        }
    }

    @Test
    public void unrelatedInterfaceFallsBackToObject(@TempDir Path root) throws Exception {
        try (SuperClassResolver resolver = strategy.apply(context(root, emptyClasspath()))) {
            assertEquals("java/lang/Object", resolver.getCommonSuperClass("java/lang/Runnable", "java/lang/Integer"));
        }
    }

    @Test
    public void loadsClassesFromContextClasspath(@TempDir Path root) throws Exception {
        Path jar = TestClasses.writeJar(root.resolve("lib.jar"), Map.of(
            "test/Base.class", TestClasses.classBytes("test/Base", "java/lang/Object"),
            "test/A.class", TestClasses.classBytes("test/A", "test/Base"),
            "test/B.class", TestClasses.classBytes("test/B", "test/Base")
        ));
        Classpath classpath = new Classpath(new LinkedHashSet<>(List.of(jar)), new LinkedHashSet<>());

        try (SuperClassResolver resolver = strategy.apply(context(root, classpath))) {
            assertEquals("test/Base", resolver.getCommonSuperClass("test/A", "test/B"));
        }
    }

    @Test
    public void throwsForUnknownClasses(@TempDir Path root) throws Exception {
        try (SuperClassResolver resolver = strategy.apply(context(root, emptyClasspath()))) {
            assertThrows(PatchException.class,
                () -> resolver.getCommonSuperClass("does/not/Exist", "java/lang/Integer"));
            assertThrows(PatchException.class,
                () -> resolver.getCommonSuperClass("java/lang/Integer", "does/not/Exist"));
        }
    }

    @Test
    public void isApplicableOutsideNativeImage() {
        assertTrue(strategy.isApplicable());
        assertEquals(SuperClassStrategy.SORT_CLASS_LOADER, strategy.sort());
    }

    private static Classpath emptyClasspath() {
        return new Classpath(new LinkedHashSet<>(), new LinkedHashSet<>());
    }

}
