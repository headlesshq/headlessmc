package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.version.SettingsProcessScanner;
import io.github.headlesshq.headlessmc.java.version.SimpleProcessScanner;
import io.github.headlesshq.headlessmc.java.version.SpecificationVersionParser;
import io.github.headlesshq.headlessmc.os.OS;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The sources are constructed directly rather than injected, because every test
 * gives them a different {@link io.github.headlesshq.headlessmc.java.JavaConfig}
 * or {@link OS} - only the scanning machinery they share comes from the container.
 */
@QuarkusComponentTest({
    JavaScannerService.class,
    JavaExecutableFinder.class,
    JavaHomeFinder.class,
    SettingsProcessScanner.class,
    SimpleProcessScanner.class,
    SpecificationVersionParser.class,
    TestBeans.class
})
public class JavaSourcesTest {
    @Inject
    JavaExecutableFinder executableFinder;

    @Inject
    JavaScannerService javaScannerService;

    @Inject
    SpecificationVersionParser versionParser;

    @Test
    public void currentSourceFindsRunningJvm() {
        CurrentJavaSource currentJavaSource = new CurrentJavaSource(versionParser, executableFinder);
        List<Java> javas = currentJavaSource.getJavas();

        assertEquals(1, javas.size());
        Java java = javas.getFirst();
        assertEquals(Runtime.version().feature(), java.version());
        assertTrue(java.current());
        assertEquals(JavaSource.SORT_CURRENT, java.source());
        assertEquals("current", currentJavaSource.getName());
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = FakeJdks.WINDOWS_REASON)
    public void configSourceScansConfiguredDirs(@TempDir Path dir) throws Exception {
        Path jdk = FakeJdks.create(dir.resolve("jdk-17"), 17);
        ConfigJavaSource source = new ConfigJavaSource(javaScannerService, FakeJavaConfig.holder(List.of(jdk)));

        List<Java> javas = source.getJavas();

        assertEquals(1, javas.size());
        assertEquals(17, javas.getFirst().version());
        assertEquals(JavaSource.SORT_CONFIG, javas.getFirst().source());
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = FakeJdks.WINDOWS_REASON)
    public void configSourceMapsExecutablePathToHome(@TempDir Path dir) throws Exception {
        Path jdk = FakeJdks.create(dir.resolve("jdk-21"), 21);
        Path executable = jdk.resolve("bin").resolve("java");
        ConfigJavaSource source = new ConfigJavaSource(javaScannerService, FakeJavaConfig.holder(List.of(executable)));

        List<Java> javas = source.getJavas();

        assertEquals(1, javas.size());
        assertEquals(jdk, javas.getFirst().home().getUnchecked().orElseThrow());
    }

    @Test
    public void configSourceThrowsOnDirWithoutJava(@TempDir Path dir) {
        ConfigJavaSource source = new ConfigJavaSource(javaScannerService, FakeJavaConfig.holder(List.of(dir)));
        assertThrows(HeadlessMcIOException.class, source::getJavas);
    }

    @Test
    public void configSourceIsEmptyWithoutConfiguredVersions() {
        ConfigJavaSource source = new ConfigJavaSource(javaScannerService, FakeJavaConfig.holder(List.of()));
        assertTrue(source.getJavas().isEmpty());
    }

    @Test
    public void linuxSourceIsEmptyOnOtherOs() {
        LinuxJavaSource source = new LinuxJavaSource(
            javaScannerService,
            new OS("windows", OS.Type.WINDOWS, "10")
        );
        assertTrue(source.getJavas().isEmpty());
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = FakeJdks.WINDOWS_REASON)
    public void linuxSourceResolvesNixProfiles(@TempDir Path dir) throws Exception {
        Path jdk = FakeJdks.create(dir.resolve("nix/store/abc-openjdk-21/lib/openjdk"), 21);
        Path systemProfile = dir.resolve("run/current-system/sw");
        Path userProfile = dir.resolve("home/.nix-profile");
        for (Path profile : List.of(systemProfile, userProfile)) {
            Files.createDirectories(profile.resolve("bin"));
            Files.createSymbolicLink(profile.resolve("bin").resolve("java"), jdk.resolve("bin").resolve("java"));
        }

        LinuxJavaSource source = new LinuxJavaSource(javaScannerService, new OS("linux", OS.Type.LINUX, "6.0"));
        List<Java> javas = source.scanNixAndPathExecutables(
            Stream.of(systemProfile, userProfile, dir.resolve("missing")).map(p -> p.resolve("bin").resolve("java")).toList(),
            Set.of()
        );

        assertEquals(1, javas.size());
        assertEquals(21, javas.getFirst().version());
        assertEquals(jdk.toRealPath(), javas.getFirst().home().getUnchecked().orElseThrow());
        assertEquals(JavaSource.SORT_GENERAL, javas.getFirst().source());
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = FakeJdks.WINDOWS_REASON)
    public void linuxSourceResolvesJavaHomeAndPath(@TempDir Path dir) throws Exception {
        Path shellJdk = FakeJdks.create(dir.resolve("nix/store/abc-openjdk-21/lib/openjdk"), 21);
        Path systemJdk = FakeJdks.create(dir.resolve("usr/lib/jvm/java-17"), 17);
        Path shellBin = dir.resolve("nix/store/abc-openjdk-21/bin");
        Files.createDirectories(shellBin);
        Files.createSymbolicLink(shellBin.resolve("java"), shellJdk.resolve("bin").resolve("java"));
        Path usrBin = dir.resolve("usr/bin");
        Files.createDirectories(usrBin);
        Files.createSymbolicLink(usrBin.resolve("java"), systemJdk.resolve("bin").resolve("java"));

        List<Path> executables = LinuxJavaSource.environmentExecutables(
            shellJdk.toString(),
            String.join(File.pathSeparator, shellBin.toString(), "", dir.resolve("missing").toString(), usrBin.toString())
        );
        assertEquals(List.of(
            shellJdk.resolve("bin").resolve("java"),
            shellBin.resolve("java"),
            dir.resolve("missing").resolve("java"),
            usrBin.resolve("java")
        ), executables);

        LinuxJavaSource source = new LinuxJavaSource(javaScannerService, new OS("linux", OS.Type.LINUX_MUSL, "6.0"));
        List<Java> javas = source.scanNixAndPathExecutables(executables, Set.of(systemJdk.toRealPath()));

        assertEquals(1, javas.size());
        assertEquals(21, javas.getFirst().version());
        assertEquals(shellJdk.toRealPath(), javas.getFirst().home().getUnchecked().orElseThrow());
    }

    @Test
    public void linuxSourceIgnoresMissingEnvironment() {
        assertTrue(LinuxJavaSource.environmentExecutables(null, null).isEmpty());
        assertTrue(LinuxJavaSource.environmentExecutables(" ", "").isEmpty());
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = FakeJdks.WINDOWS_REASON)
    public void headlessMcSourceScansNestedInstallations(@TempDir Path dir) throws Exception {
        AppFiles appFiles = TestFiles.appFiles(dir);
        // e.g. OpenJDK8U-jre_x64_mac_hotspot_8u502b07/jdk8u502-b07-jre/Contents/Home on macOS
        FakeJdks.create(
            appFiles.getJavaDir().resolve("temurin-21").resolve("jdk-21.0.1").resolve("Contents").resolve("Home"),
            21
        );
        HeadlessMcJavaSource source = new HeadlessMcJavaSource(
            javaScannerService, FakeJavaConfig.holder(), appFiles
        );

        List<Java> javas = source.getJavas();

        assertEquals(1, javas.size());
        assertEquals(21, javas.getFirst().version());
        assertEquals("temurin-21", javas.getFirst().name());
        assertEquals(JavaSource.SORT_HMC, javas.getFirst().source());
    }

    @Test
    public void windowsSourceIsEmptyOnOtherOs() {
        WindowsJavaSource source = new WindowsJavaSource(
            javaScannerService,
            new OS("linux", OS.Type.LINUX, "6.0")
        );
        assertTrue(source.getJavas().isEmpty());
    }

}
