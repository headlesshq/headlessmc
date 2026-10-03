package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.TemplateString;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LibraryResolverTest {
    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final OS linux = new OS("linux", OS.Type.LINUX, "6.0");
    private final CPU cpu = CPU.X64;

    private LibraryResolver resolver(McFiles mcFiles) {
        return new LibraryResolver(
            new NativeLibraryResolver(fileService, mcFiles, linux),
            fileService,
            mcFiles,
            cpu,
            linux
        );
    }

    private List<LibraryFile> resolve(McFiles mcFiles, TestLibraries.FakeLibrary... libraries) {
        return resolve(mcFiles, new TemplateStrings(), libraries);
    }

    private List<LibraryFile> resolve(
        McFiles mcFiles,
        TemplateStrings templates,
        TestLibraries.FakeLibrary... libraries
    ) {
        return resolver(mcFiles).resolveLibraries(
            templates,
            new Features(),
            new TestLibraries.FakeVersion(List.of(libraries))
        );
    }

    @Test
    public void resolvesLibraryWithoutDownloadsToMavenPath(@TempDir Path root) {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");

        List<LibraryFile> files = resolve(mcFiles, library);

        assertEquals(1, files.size());
        assertEquals(Artifact.of("com.example:lib:1.0").jar(mcFiles.getLibraryDir()), files.getFirst().path());
        assertNull(files.getFirst().download());
    }

    @Test
    public void skipsLibrariesDisallowedByRules(@TempDir Path root) {
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");
        library.rules = List.of(TestLibraries.rule(Version.Rule.ALLOW, "windows"));

        assertTrue(resolve(TestLibraries.mcFiles(root), library).isEmpty());
    }

    @Test
    public void usesArtifactDownloadPath(@TempDir Path root) {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");
        Version.Download artifact = TestLibraries.download("com/example/lib/1.0/lib-1.0.jar", 100L);
        library.downloads = TestLibraries.downloads(artifact, null);

        List<LibraryFile> files = resolve(mcFiles, library);

        assertEquals(1, files.size());
        assertEquals(
            mcFiles.getLibraryDir().resolve("com").resolve("example").resolve("lib").resolve("1.0")
                .resolve("lib-1.0.jar"),
            files.getFirst().path()
        );
        assertEquals(artifact, files.getFirst().download());
    }

    @Test
    public void skipsEmptyJarArtifacts(@TempDir Path root) {
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");
        library.downloads = TestLibraries.downloads(
            TestLibraries.download("com/example/lib/1.0/lib-1.0.jar", 22L), null);

        assertTrue(resolve(TestLibraries.mcFiles(root), library).isEmpty());
    }

    @Test
    public void resolvesNativeClassifierForOs(@TempDir Path root) {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("org.lwjgl:lwjgl-platform:2.9.4");
        Version.Download natives = TestLibraries.download(
            "org/lwjgl/lwjgl-platform/2.9.4/lwjgl-platform-2.9.4-natives-linux.jar", 1000L);
        library.natives = Map.of("linux", "natives-linux");
        library.downloads = TestLibraries.downloads(
            TestLibraries.download("org/lwjgl/lwjgl-platform/2.9.4/lwjgl-platform-2.9.4.jar", 22L),
            Map.of("natives-linux", natives)
        );

        List<LibraryFile> files = resolve(mcFiles, library);

        assertEquals(1, files.size(), "empty artifact jar should be skipped, natives resolved");
        assertEquals(natives, files.getFirst().download());
        assertTrue(files.getFirst().path().endsWith("lwjgl-platform-2.9.4-natives-linux.jar"));
    }

    @Test
    public void processesTemplatesInNativeClassifiers(@TempDir Path root) {
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("org.lwjgl:lwjgl-platform:2.9.4");
        Version.Download natives = TestLibraries.download(
            "org/lwjgl/lwjgl-platform/2.9.4/lwjgl-platform-2.9.4-natives-linux-64.jar", 1000L);
        library.natives = Map.of("linux", "natives-linux-${arch}");
        library.downloads = TestLibraries.downloads(null, Map.of("natives-linux-64", natives));
        TemplateStrings templates = new TemplateStrings();
        templates.add(TemplateString.ARCH, "64");

        List<LibraryFile> files = resolve(TestLibraries.mcFiles(root), templates, library);

        assertEquals(1, files.size());
        assertEquals(natives, files.getFirst().download());
    }

    @Test
    public void missingNativesEntryThrows(@TempDir Path root) {
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");
        library.downloads = TestLibraries.downloads(null, Map.of("natives-linux", TestLibraries.download("p", 1L)));

        assertThrows(LibraryException.class, () -> resolve(TestLibraries.mcFiles(root), library));
    }

    @Test
    public void missingNativesForOsThrows(@TempDir Path root) {
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");
        library.natives = Map.of("windows", "natives-windows");
        library.downloads = TestLibraries.downloads(null, Map.of("natives-windows", TestLibraries.download("p", 1L)));

        assertThrows(LibraryException.class, () -> resolve(TestLibraries.mcFiles(root), library));
    }

    @Test
    public void missingClassifierDownloadThrows(@TempDir Path root) {
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");
        library.natives = Map.of("linux", "natives-linux");
        library.downloads = TestLibraries.downloads(null, Map.of("natives-osx", TestLibraries.download("p", 1L)));

        assertThrows(LibraryException.class, () -> resolve(TestLibraries.mcFiles(root), library));
    }

    @Test
    public void nativeDownloadWithoutPathThrows(@TempDir Path root) {
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("com.example:lib:1.0");
        library.natives = Map.of("linux", "natives-linux");
        library.downloads = TestLibraries.downloads(null, Map.of("natives-linux", TestLibraries.download(null, 1L)));

        assertThrows(LibraryException.class, () -> resolve(TestLibraries.mcFiles(root), library));
    }

}
