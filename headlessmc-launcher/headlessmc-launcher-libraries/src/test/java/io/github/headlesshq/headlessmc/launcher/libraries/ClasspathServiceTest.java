package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.TemplateString;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClasspathServiceTest {
    private static final URI MC_JAR_URL = URI.create("https://launcher.mojang.com/client.jar");
    private static final byte[] MC_JAR = "mc-jar".getBytes(StandardCharsets.UTF_8);

    private record TestDownload(
        @Nullable String getId,
        @Nullable String getSha1,
        @Nullable Long getSize,
        @Nullable String getUrl,
        @Nullable String getPath
    ) implements Version.Download {}

    @TempDir
    Path root;

    private final MockDownloadService downloads = new MockDownloadService();
    private final List<List<LibraryFile>> downloadedLibraries = new ArrayList<>();

    private McFiles mcFiles;
    private ClasspathService service;
    private List<LibraryFile> resolved;

    @BeforeEach
    void setup() {
        mcFiles = TestLibraries.mcFiles(root);
        resolved = new ArrayList<>();
        downloads.register(MC_JAR_URL, MC_JAR);

        LibraryResolver resolver = Mockito.mock(LibraryResolver.class);
        Mockito.when(resolver.resolveLibraries(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(invocation -> resolved);

        LibraryDownloader downloader = Mockito.mock(LibraryDownloader.class);
        Mockito.doAnswer(invocation -> downloadedLibraries.add(invocation.getArgument(1)))
            .when(downloader).download(Mockito.any(), Mockito.any());

        NativeLibraryHandler natives = Mockito.mock(NativeLibraryHandler.class);
        Mockito.when(natives.handleNativeLibraries(Mockito.any())).thenReturn(root.resolve("natives"));

        service = new ClasspathService(downloader, resolver, natives, new McJarDownloader(downloads, mcFiles));
    }

    private Version version() {
        return new io.github.headlesshq.headlessmc.version.FakeVersion("1.21.1").withDownload(
            Version.DOWNLOAD_CLIENT,
            new TestDownload(null, null, (long) MC_JAR.length, MC_JAR_URL.toString(), null)
        );
    }

    @Test
    void theClasspathContainsTheLibrariesAndTheMcJar() throws Exception {
        Path libraryJar = Files.createFile(Files.createDirectories(root.resolve("libs")).resolve("a.jar"));
        resolved.add(new LibraryFile(new TestLibraries.FakeLibrary("com.example:a:1.0"), libraryJar, null));

        TemplateStrings templates = new TemplateStrings();
        List<Path> classpath = service.buildClasspath(templates, new Features(), version(), List.of());

        Path mcJar = mcFiles.getVersionsDir().resolve("1.21.1").resolve("1.21.1.jar");
        assertEquals(List.of(libraryJar, mcJar), classpath);
        assertArrayEquals(MC_JAR, Files.readAllBytes(mcJar));
        assertEquals(List.of(resolved), downloadedLibraries);
        assertEquals(
            root.resolve("natives").toAbsolutePath().toString(),
            templates.process("${" + TemplateString.NATIVES_DIRECTORY.name() + "}")
        );
    }

    @Test
    void withoutLibrariesTheClasspathIsJustTheMcJar() {
        List<Path> classpath = service.buildClasspath(new TemplateStrings(), new Features(), version(), List.of());

        assertEquals(1, classpath.size());
    }

}
