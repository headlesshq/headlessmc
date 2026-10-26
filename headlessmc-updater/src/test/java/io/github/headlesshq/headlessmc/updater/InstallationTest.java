package io.github.headlesshq.headlessmc.updater;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InstallationTest {
    private static final Path JAR = Path.of("hmc/headlessmc.jar").toAbsolutePath();

    @Test
    public void testNative() {
        Installation installation = Installation.detect(
            Map.of("org.graalvm.nativeimage.imagecode", "runtime", "java.class.path", "")::get,
            Optional.of("hmc/headlessmc-launcher-linux-x64"),
            Optional.empty()
        );

        assertEquals(Installation.Type.NATIVE, installation.type());
        assertEquals(Path.of("hmc/headlessmc-launcher-linux-x64").toAbsolutePath(), installation.path());
    }

    @Test
    public void testNativeWithoutCommand() {
        Installation installation = Installation.detect(
            Map.of("org.graalvm.nativeimage.imagecode", "runtime")::get,
            Optional.empty(),
            Optional.empty()
        );

        assertEquals(new Installation(Installation.Type.NATIVE, null), installation);
    }

    @Test
    public void testUberJar() {
        Installation installation = detect("hmc/headlessmc.jar", Optional.of(Path.of("hmc/../hmc/headlessmc.jar")));
        assertEquals(new Installation(Installation.Type.JAR, JAR), installation);
    }

    @Test
    public void testFastJar() {
        assertUnknown(detect(
            "build/quarkus-app/quarkus-run.jar",
            Optional.of(Path.of("build/quarkus-app/lib/main/io.github.headlesshq.headlessmc-updater-3.0.0.jar"))
        ));
        assertUnknown(detect("build/quarkus-app/quarkus-run.jar", Optional.empty()));
    }

    @Test
    public void testUnknown() {
        assertUnknown(detect("a.jar:b.jar", Optional.of(Path.of("a.jar"))));
        assertUnknown(detect("build/classes/java/main", Optional.of(Path.of("build/classes/java/main"))));
        assertUnknown(Installation.detect(Map.<String, String>of()::get, Optional.empty(), Optional.empty()));
    }

    private static Installation detect(String classPath, Optional<Path> codeSource) {
        return Installation.detect(
            Map.of("java.class.path", classPath, "path.separator", ":")::get,
            Optional.of("/usr/bin/java"),
            codeSource
        );
    }

    private static void assertUnknown(Installation installation) {
        assertEquals(new Installation(Installation.Type.UNKNOWN, null), installation);
    }

}
