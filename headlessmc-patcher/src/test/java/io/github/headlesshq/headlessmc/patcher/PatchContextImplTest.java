package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PatchContextImplTest {
    @Inject
    FileService fileService;

    private interface FirstService extends HelperService {
    }

    private interface SecondService extends HelperService {
    }

    private PatchContextImpl context(Path root, Classpath classpath, List<HelperService> services) {
        return new PatchContextImpl(
            services::stream,
            classpath,
            fileService,
            new PatchCache.Key("abc", 1L, Map.of(), 21, 0L),
            List.of(TestPatchers.patcher("test", 1L)),
            TestPatchers.mcFiles(root),
            21,
            root.resolve("base"),
            classpath
        );
    }

    @Test
    public void addPutsNewFileOnClasspathOnClose(@TempDir Path root) throws IOException {
        PatchContextImpl context = context(root, new Classpath(new LinkedHashSet<>(), new LinkedHashSet<>()), List.of());
        Patcher patcher = TestPatchers.patcher("adder", 1L);

        try (OutputStream out = context.add("extra", patcher)) {
            out.write("content".getBytes(StandardCharsets.UTF_8));
            assertTrue(context.getCurrentClasspath().files().isEmpty(), "file should only be added on close");
        }

        Path expected = root.resolve("base").resolve("adder").resolve("extra.jar");
        assertTrue(Files.exists(expected));
        assertEquals("content", Files.readString(expected));
        assertTrue(context.getCurrentClasspath().files().contains(expected));
    }

    @Test
    public void patchReplacesLibraryOnClasspath(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(
            mcFiles.getLibraryDir().resolve("org").resolve("lib.jar"), "a.txt", "original");
        Classpath classpath = new Classpath(new LinkedHashSet<>(List.of(library)), new LinkedHashSet<>());
        PatchContextImpl context = context(root, classpath, List.of());
        Patcher patcher = TestPatchers.patcher("patcher", 1L);

        context.patch(library, patcher, (source, destination) -> {
            copy(source, destination);
            destination.putNextEntry(new JarEntry("patched.txt"));
            destination.write("patched".getBytes(StandardCharsets.UTF_8));
            destination.closeEntry();
            return true;
        });

        Path patched = root.resolve("base").resolve("patcher").resolve("org").resolve("lib.jar");
        assertTrue(Files.exists(patched));
        assertTrue(Files.exists(library), "original library outside base dir must not be deleted");
        assertEquals(new LinkedHashSet<>(List.of(patched)), context.getCurrentClasspath().files());
        try (JarFile jar = new JarFile(patched.toFile())) {
            assertNotNull(jar.getEntry("a.txt"));
            assertNotNull(jar.getEntry("patched.txt"));
        }
    }

    @Test
    public void abandonedPatchLeavesClasspathUntouched(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "original");
        Classpath classpath = new Classpath(new LinkedHashSet<>(List.of(library)), new LinkedHashSet<>());
        PatchContextImpl context = context(root, classpath, List.of());

        context.patch(library, TestPatchers.patcher("patcher", 1L), (source, destination) -> false);

        assertEquals(classpath, context.getCurrentClasspath());
        assertTrue(Files.notExists(root.resolve("base").resolve("patcher").resolve("lib.jar")));
    }

    @Test
    public void patchingTwiceDeletesIntermediateFile(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(
            mcFiles.getLibraryDir().resolve("org").resolve("lib.jar"), "a.txt", "original");
        Classpath classpath = new Classpath(new LinkedHashSet<>(List.of(library)), new LinkedHashSet<>());
        PatchContextImpl context = context(root, classpath, List.of());

        context.patch(library, TestPatchers.patcher("first", 1L), (source, destination) -> {
            copy(source, destination);
            return true;
        });
        Path intermediate = root.resolve("base").resolve("first").resolve("org").resolve("lib.jar");
        assertTrue(Files.exists(intermediate));

        context.patch(intermediate, TestPatchers.patcher("second", 1L), (source, destination) -> {
            copy(source, destination);
            return true;
        });

        Path patched = root.resolve("base").resolve("second").resolve("org").resolve("lib.jar");
        assertTrue(Files.notExists(intermediate), "intermediate patch file should be deleted");
        assertEquals(new LinkedHashSet<>(List.of(patched)), context.getCurrentClasspath().files());
    }

    @Test
    public void patchWrapsIoExceptions(@TempDir Path root) throws IOException {
        Path notAJar = Files.writeString(root.resolve("not-a-jar.jar"), "garbage");
        Classpath classpath = new Classpath(new LinkedHashSet<>(List.of(notAJar)), new LinkedHashSet<>());
        PatchContextImpl context = context(root, classpath, List.of());

        assertThrows(PatchException.class,
            () -> context.patch(notAJar, TestPatchers.patcher("patcher", 1L), (source, destination) -> true));
    }

    @Test
    public void servicesFiltersByType(@TempDir Path root) {
        FirstService first = new FirstService() {
        };
        SecondService second = new SecondService() {
        };
        PatchContextImpl context = context(root, new Classpath(new LinkedHashSet<>(), new LinkedHashSet<>()), List.of(first, second));

        assertEquals(List.of(first), context.services(FirstService.class).toList());
        assertEquals(List.of(second), context.services(SecondService.class).toList());
        assertEquals(2, context.services(HelperService.class).count());
    }

    private static void copy(JarFile source, java.util.jar.JarOutputStream destination) throws IOException {
        Enumeration<JarEntry> entries = source.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            destination.putNextEntry(new JarEntry(entry.getName()));
            try (var in = source.getInputStream(entry)) {
                in.transferTo(destination);
            }

            destination.closeEntry();
        }
    }

}
