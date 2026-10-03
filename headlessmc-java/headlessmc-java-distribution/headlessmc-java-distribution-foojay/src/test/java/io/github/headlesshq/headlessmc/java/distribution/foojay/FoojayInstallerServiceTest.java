package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.ArchiveType;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.os.OS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FoojayInstallerServiceTest {
    private static final OS LINUX = new OS("linux", OS.Type.LINUX, "6.0");
    private static final OS WINDOWS = new OS("windows", OS.Type.WINDOWS, "11");

    @TempDir
    Path root;

    private FoojayInstallerService service(OS os) {
        return Installers.installer(os);
    }

    private byte[] jdkZip() {
        return jdkZip("java");
    }

    private byte[] jdkZip(String executable) {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("jdk-21/bin/" + executable, "binary");
        entries.put("jdk-21/lib/jspawnhelper", "helper");
        return Archives.zip(entries);
    }

    @Test
    void supportsTheCommonArchiveTypes() {
        assertTrue(service(LINUX).getSupportedArchiveTypes().containsAll(
            Set.of(ArchiveType.TAR, ArchiveType.TAR_GZ, ArchiveType.TGZ, ArchiveType.TAR_Z, ArchiveType.ZIP)
        ));
    }

    @Test
    void installsKeepingTheArchiveLayout() throws IOException {
        Path target = root.resolve("java-21");
        service(LINUX).install(
            target, new JavaRuntime.MetaData(0L, "zip"), new ByteArrayInputStream(jdkZip())
        );

        Path home = target.resolve("jdk-21");
        assertEquals("binary", Files.readString(home.resolve("bin").resolve("java")));

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(home.resolve("bin").resolve("java"));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
        assertTrue(Files.getPosixFilePermissions(home.resolve("lib").resolve("jspawnhelper"))
            .contains(PosixFilePermission.OWNER_EXECUTE));
    }

    @Test
    void addsPermissionsToDeeplyNestedInstallations() throws IOException {
        Path target = root.resolve("java-8");
        Map<String, String> entries = new LinkedHashMap<>();
        String home = "OpenJDK8U-jre_x64_mac_hotspot_8u502b07/jdk8u502-b07-jre/Contents/Home/";
        entries.put(home + "bin/java", "binary");
        entries.put(home + "lib/jspawnhelper", "helper");
        service(LINUX).install(
            target, new JavaRuntime.MetaData(0L, "zip"), new ByteArrayInputStream(Archives.zip(entries))
        );

        Path javaHome = target.resolve(home);
        assertTrue(Files.getPosixFilePermissions(javaHome.resolve("bin").resolve("java"))
            .contains(PosixFilePermission.OWNER_EXECUTE));
        assertTrue(Files.getPosixFilePermissions(javaHome.resolve("lib").resolve("jspawnhelper"))
            .contains(PosixFilePermission.OWNER_EXECUTE));
    }

    @Test
    void installsWithoutChangingPermissionsOnWindows() {
        Path target = root.resolve("java-21");
        service(WINDOWS).install(
            target, new JavaRuntime.MetaData(0L, "zip"), new ByteArrayInputStream(jdkZip("java.exe"))
        );

        assertTrue(Files.exists(target.resolve("jdk-21").resolve("bin").resolve("java.exe")));
    }

    @Test
    void archiveWithBinDirAtTheRootIsInstalledAsIs() throws IOException {
        Path target = root.resolve("java-21");
        service(LINUX).install(
            target, new JavaRuntime.MetaData(0L, "zip"),
            new ByteArrayInputStream(Archives.zip(Map.of("bin/java", "binary")))
        );

        assertEquals("binary", Files.readString(target.resolve("bin").resolve("java")));
    }

    @Test
    void unsupportedArchiveTypeThrows() {
        // apk is a known ArchiveType, but has no extractor
        FileException e = assertThrows(FileException.class, () -> service(LINUX).install(
            root.resolve("java-21"),
            new JavaRuntime.MetaData(0L, ArchiveType.APK.getApiString()),
            new ByteArrayInputStream(new byte[0])
        ));
        assertTrue(e.getMessage().contains("Unsupported Archive Type"));
    }

    @Test
    void archiveWithoutInstallationDirThrows() {
        assertThrows(FileException.class, () -> service(LINUX).install(
            root.resolve("java-21"), new JavaRuntime.MetaData(0L, "zip"),
            new ByteArrayInputStream(Archives.zip(Map.of("readme.txt", "no jdk here")))
        ));
    }

}
