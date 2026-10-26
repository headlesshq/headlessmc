package io.github.headlesshq.headlessmc.updater;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InstallationTest {
    @Test
    public void testNative() {
        Installation installation = Installation.detect(
            Map.of("org.graalvm.nativeimage.imagecode", "runtime", "java.class.path", "")::get,
            Optional.of("hmc/headlessmc-launcher-linux-x64")
        );

        assertEquals(Installation.Type.NATIVE, installation.type());
        assertEquals(Path.of("hmc/headlessmc-launcher-linux-x64").toAbsolutePath(), installation.path());
    }

    @Test
    public void testNativeWithoutCommand() {
        Installation installation = Installation.detect(
            Map.of("org.graalvm.nativeimage.imagecode", "runtime")::get,
            Optional.empty()
        );

        assertEquals(new Installation(Installation.Type.NATIVE, null), installation);
    }

    @Test
    public void testJar() {
        Installation installation = Installation.detect(
            Map.of("java.class.path", "hmc/headlessmc.jar", "path.separator", ":")::get,
            Optional.of("/usr/bin/java")
        );

        assertEquals(Installation.Type.JAR, installation.type());
        assertEquals(Path.of("hmc/headlessmc.jar").toAbsolutePath(), installation.path());
    }

    @Test
    public void testUnknown() {
        assertUnknown("build/quarkus-app/quarkus-run.jar");
        assertUnknown("a.jar:b.jar");
        assertUnknown("build/classes/java/main");
        assertEquals(Installation.Type.UNKNOWN, Installation.detect(Map.<String, String>of()::get, Optional.empty()).type());
    }

    private static void assertUnknown(String classPath) {
        Installation installation = Installation.detect(
            Map.of("java.class.path", classPath, "path.separator", ":")::get,
            Optional.of("/usr/bin/java")
        );

        assertEquals(new Installation(Installation.Type.UNKNOWN, null), installation, classPath);
    }

}
