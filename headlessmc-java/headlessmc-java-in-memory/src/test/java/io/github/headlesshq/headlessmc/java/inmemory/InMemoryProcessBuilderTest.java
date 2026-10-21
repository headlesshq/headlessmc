package io.github.headlesshq.headlessmc.java.inmemory;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.java.inmemory.programs.Programs;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryProcessBuilderTest {
    private static final Java CURRENT = new Java("current", 17, new SafePath(null), new SafePath(null), true, 0);

    @AfterEach
    void clearProperties() {
        for (String key : List.of("hmc.test.echo", "hmc.test.loader", "hmc.test.property", "hmc.test.thread")) {
            System.clearProperty(key);
        }
    }

    @Test
    void runsMainClassWithArgsAndSystemProperties() {
        JavaProcess process = builder()
            .classpath(programs())
            .jvmArg("-Xmx1G", "-Dhmc.test.property=jvmArg")
            .systemProperty("hmc.test.property", "builder")
            .mainClass(Programs.Echo.class.getName())
            .arg("a", "b")
            .version(17)
            .start();

        assertEquals(0, process.waitFor(Duration.ofSeconds(10)));
        // systemProperty overrides jvm args, like a later -D
        assertEquals("a,b:builder", System.getProperty("hmc.test.echo"));
        assertEquals("true", System.getProperty("hmc.test.loader"));
        assertEquals(Optional.empty(), process.getProcess());
        assertEquals(Optional.of(CURRENT), process.getJava());
    }

    @Test
    void classIsLoadedInSeparateClassLoader() throws Exception {
        JavaProcess process = builder().classpath(programs()).mainClass(Programs.Echo.class.getName()).start();
        process.waitFor(Duration.ofSeconds(10));
        assertNotSame(
            Programs.Echo.class,
            ((InMemoryJavaProcess) process).getClassLoader().loadClass(Programs.Echo.class.getName())
        );
    }

    @Test
    void classpathFromJvmArgs() {
        JavaProcess process = builder()
            .jvmArg("-cp", programs().toString())
            .classpathArgProvided(true)
            .mainClass(Programs.Echo.class.getName())
            .start();

        assertEquals(0, process.waitFor(Duration.ofSeconds(10)));
        assertEquals(":null", System.getProperty("hmc.test.echo"));
    }

    @Test
    void exceptionInMainExitsWithOne() {
        JavaProcess process = builder().classpath(programs()).mainClass(Programs.Throws.class.getName()).start();
        assertEquals(1, process.waitFor(Duration.ofSeconds(10)));
    }

    @Test
    void missingMainClassExitsWithOne() {
        JavaProcess process = builder().classpath(programs()).mainClass("does.not.Exist").start();
        assertEquals(1, process.waitFor(Duration.ofSeconds(10)));
    }

    @Test
    void waitsForNonDaemonThreads() {
        JavaProcess process = builder()
            .classpath(programs())
            .mainClass(Programs.StartsThread.class.getName())
            .arg("300")
            .start();

        assertEquals(0, process.waitFor(Duration.ofSeconds(10)));
        assertEquals("done", System.getProperty("hmc.test.thread"));
    }

    @Test
    void timeoutAndKill() {
        JavaProcess process = builder()
            .classpath(programs())
            .mainClass(Programs.StartsThread.class.getName())
            .arg("60000")
            .start();

        assertThrows(JavaProcessException.class, () -> process.waitFor(Duration.ofMillis(100)));
        process.kill();
        assertEquals(0, process.waitFor(Duration.ofSeconds(10)));
        assertEquals("interrupted", System.getProperty("hmc.test.thread"));
    }

    @Test
    void runsJarWithManifest(@TempDir Path dir) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Programs.Echo.class.getName());
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, dir.relativize(programs()).toString() + "/");
        Path jar = dir.resolve("echo.jar");
        try (OutputStream os = Files.newOutputStream(jar); JarOutputStream ignored = new JarOutputStream(os, manifest)) {
            // the jar only contains the manifest, the classes come from its Class-Path
        }

        JavaProcess process = builder().jar(jar).arg("jar").start();
        assertEquals(0, process.waitFor(Duration.ofSeconds(10)));
        assertEquals("jar:null", System.getProperty("hmc.test.echo"));
        assertEquals(Optional.of(Programs.Echo.class.getName()), process.getMainClass());
    }

    @Test
    void jarWithoutMainClassFails(@TempDir Path dir) throws Exception {
        Path jar = dir.resolve("empty.jar");
        try (OutputStream os = Files.newOutputStream(jar); JarOutputStream ignored = new JarOutputStream(os, new Manifest())) {
            // no Main-Class
        }

        assertThrows(JavaProcessException.class, () -> builder().jar(jar).start());
    }

    @Test
    void requiresJarOrMainClass() {
        assertThrows(IllegalStateException.class, () -> builder().classpath(programs()).start());
    }

    private static InMemoryProcessBuilder builder() {
        return new InMemoryProcessBuilder(new JavaService() {
            @Override
            public Optional<Java> getJava(int version) {
                return Optional.of(CURRENT);
            }

            @Override
            public List<Java> getJavaVersions() {
                return List.of(CURRENT);
            }

            @Override
            public List<JavaSource> getSources() {
                return List.of();
            }

            @Override
            public void refresh() {
            }
        });
    }

    private static Path programs() {
        try {
            return Path.of(Programs.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
