package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.version.SettingsProcessScanner;
import io.github.headlesshq.headlessmc.java.version.SimpleProcessScanner;
import io.github.headlesshq.headlessmc.java.version.SpecificationVersionParser;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest({
    JavaScannerService.class,
    JavaExecutableFinder.class,
    JavaHomeFinder.class,
    SettingsProcessScanner.class,
    SimpleProcessScanner.class,
    SpecificationVersionParser.class,
    TestBeans.class
})
public class JavaScannerServiceTest {
    @Inject
    JavaScannerService service;

    private final JavaSource source = new JavaSource() {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        public List<Java> getJavas() {
            return List.of();
        }

        @Override
        public int sort() {
            return JavaSource.SORT_DEFAULT;
        }
    };

    @Test
    public void scansFakeJdkDir(@TempDir Path dir) throws Exception {
        Path jdk = FakeJdks.create(dir.resolve("jdk-17"), 17);

        Optional<Java> java = service.scanDir(source, jdk);

        assertTrue(java.isPresent());
        assertEquals(17, java.get().version());
        assertEquals("jdk-17", java.get().name());
        assertFalse(java.get().current());
        assertEquals(JavaSource.SORT_DEFAULT, java.get().source());
        assertEquals(jdk, java.get().home().getUnchecked().orElseThrow());
        assertEquals(jdk.resolve("bin").resolve("java"), java.get().executable().getUnchecked().orElseThrow());
    }

    @Test
    public void returnsEmptyWithoutExecutable(@TempDir Path dir) {
        assertTrue(service.scanDir(source, dir).isEmpty());
    }

    @Test
    public void throwsWhenAllScannersFail(@TempDir Path dir) throws Exception {
        Path jdk = FakeJdks.createBroken(dir.resolve("broken"));

        HeadlessMcException e = assertThrows(HeadlessMcException.class, () -> service.scanDir(source, jdk));
        assertTrue(e.getSuppressed().length > 0);
    }

    @Test
    public void scanDirsSkipsFilesAndFailingDirs(@TempDir Path dir) throws Exception {
        Path good = FakeJdks.create(dir.resolve("jdk-21"), 21);
        Path broken = FakeJdks.createBroken(dir.resolve("broken"));
        Path file = Files.createFile(dir.resolve("not-a-dir"));

        List<Java> result = service.scanDirs(source, Stream.of(file, good, broken));

        assertEquals(1, result.size());
        assertEquals(21, result.getFirst().version());
    }

    @Test
    public void scansSubDirs(@TempDir Path dir) throws Exception {
        FakeJdks.create(dir.resolve("jdk-8"), 8);
        FakeJdks.create(dir.resolve("jdk-17"), 17);
        Files.createFile(dir.resolve("stray-file"));

        List<Java> result = service.scanSubDirsOf(source, dir);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(java -> java.version() == 8));
        assertTrue(result.stream().anyMatch(java -> java.version() == 17));
    }

    @Test
    public void scanSubDirsOfMissingDirIsEmpty(@TempDir Path dir) {
        assertTrue(service.scanSubDirsOf(source, dir.resolve("does-not-exist")).isEmpty());
    }

    @Test
    public void doesNotScanNestedDirsWithoutDepth(@TempDir Path dir) throws Exception {
        FakeJdks.create(dir.resolve("jdk-17").resolve("Contents").resolve("Home"), 17);

        assertTrue(service.scanDir(source, dir.resolve("jdk-17")).isEmpty());
        assertTrue(service.scanSubDirsOf(source, dir).isEmpty());
    }

    @Test
    public void scansNestedInstallationDirs(@TempDir Path dir) throws Exception {
        // e.g. OpenJDK8U-jre_x64_mac_hotspot_8u502b07/jdk8u502-b07-jre/Contents/Home on macOS
        Path installation = dir.resolve("jre-8");
        Path home = FakeJdks.create(
            installation.resolve("OpenJDK8U-jre_x64_mac_hotspot_8u502b07")
                .resolve("jdk8u502-b07-jre")
                .resolve("Contents")
                .resolve("Home"),
            8
        );

        List<Java> result = service.scanSubDirsOf(source, dir, FakeJavaConfig.MAX_SCAN_DEPTH);

        assertEquals(1, result.size());
        Java java = result.getFirst();
        assertEquals(8, java.version());
        // the installation dir names the java, but its home is the nested dir
        assertEquals("jre-8", java.name());
        assertEquals(home, java.home().getUnchecked().orElseThrow());
        assertEquals(home.resolve("bin").resolve("java"), java.executable().getUnchecked().orElseThrow());
    }

}
