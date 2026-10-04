package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileSystemProvider;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest(JavaLauncherServiceImpl.class)
class ProcessLaunchTest {
    /** Entry point launched as a real child process by these tests. */
    public static final class Main {
        public static void main(String[] args) throws InterruptedException {
            if (args.length > 0 && "sleep".equals(args[0])) {
                Thread.sleep(60_000L);
                return;
            }

            System.out.println("property=" + System.getProperty("hmc.test.property"));
            System.out.println("dir=" + System.getProperty("user.dir"));
            if (args.length > 0) {
                System.exit(Integer.parseInt(args[0]));
            }
        }
    }

    /** The service only needs a {@link FileSystemProvider}, everything else it injects is mocked. */
    static class Beans {
        @Produces
        @Singleton
        FileSystemProvider fileSystemProvider() {
            return new DefaultFileSystemProvider();
        }
    }

    @TempDir
    Path root;

    @Inject
    JavaLauncherServiceImpl service;

    @InjectMock
    JavaFinder javaFinder;

    private Java currentJava;

    @BeforeEach
    void setup() {
        Path home = Path.of(System.getProperty("java.home"));
        Path executable = home.resolve("bin").resolve("java");
        currentJava = new Java(
            "current", Runtime.version().feature(), new SafePath(home), new SafePath(executable), true, 0
        );
    }

    private JavaProcessBuilder builder() {
        return service.buildProcess()
            .java(currentJava)
            .classpath(System.getProperty("java.class.path"))
            .mainClass(Main.class.getName());
    }

    @Test
    void serviceExposesItsName() {
        assertEquals("default", service.getName());
    }

    @Test
    void startsProcessAndWaitsForExitCode() {
        JavaProcess process = builder().id("test").pipeIO(true).start();

        assertEquals("test", process.getId());
        assertEquals(0, process.waitFor(ProcessHandler.defaultHandler()));
        assertEquals(Optional.of(Main.class.getName()), process.getMainClass());
        assertEquals(Optional.of(currentJava), process.getJava());
        assertTrue(process.getProcess().isPresent());
    }

    @Test
    void systemPropertiesAndDirectoryReachTheProcess() throws IOException {
        Path dir = root.resolve("work");
        JavaProcess process = builder()
            .systemProperty("hmc.test.property", "hello")
            .directory(dir)
            .pipeIO(true)
            .start();

        String output = read(process.getProcess().orElseThrow().getInputStream());
        assertEquals(0, process.waitFor((ProcessHandler) null));
        assertTrue(output.contains("property=hello"));
        assertTrue(Files.isDirectory(dir));
        assertEquals(Optional.of(dir), process.getDirectory());
    }

    @Test
    void nonZeroExitCodeIsReportedByDefaultHandler() {
        JavaProcess process = builder().arg("3").pipeIO(true).start();

        JavaProcessException e = assertThrows(
            JavaProcessException.class, () -> process.waitFor(ProcessHandler.defaultHandler())
        );
        assertTrue(e.getMessage().contains("failed with code 3"));
    }

    @Test
    void waitForTimesOut() {
        JavaProcess process = builder().arg("sleep").pipeIO(true).start();
        try {
            assertThrows(JavaProcessException.class, () -> process.waitFor(Duration.ofMillis(200L)));
        } finally {
            process.killForcibly();
        }
    }

    @Test
    void killedProcessExitsNonZero() {
        JavaProcess process = builder().arg("sleep").pipeIO(true).start();
        process.kill();

        assertNotEquals(0, process.waitFor((Duration) null));
    }

    @Test
    void javaWithoutPathOnFilesystemThrows() {
        Java broken = new Java("broken", 21, new SafePath(null), new SafePath(null), false, 0);
        JavaProcessException e = assertThrows(
            JavaProcessException.class, () -> service.buildProcess().java(broken).mainClass("Main").start()
        );
        assertTrue(e.getMessage().contains("is not available on filesystem"));
    }

    @Test
    void missingJavaAndVersionThrows() {
        JavaProcessException e = assertThrows(
            JavaProcessException.class, () -> service.buildProcess().mainClass("Main").start()
        );
        assertTrue(e.getMessage().contains("no java version specified"));
    }

    @Test
    void versionIsResolvedByJavaFinder() {
        Mockito.when(javaFinder.findJava(21)).thenReturn(currentJava);

        JavaProcess process = service.buildProcess()
            .version(21)
            .classpath(System.getProperty("java.class.path"))
            .mainClass(Main.class.getName())
            .pipeIO(true)
            .start();

        assertEquals(0, process.waitFor((Duration) null));
    }

    @Test
    void processWithoutProcessReturnsZero() {
        JavaProcessImpl process = new JavaProcessImpl(
            "id", java.util.Map.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty()
        );

        assertEquals(0, process.waitFor((Duration) null));
        assertEquals(0, process.waitFor(ProcessHandler.defaultHandler()));
        process.kill();
        process.killForcibly();
    }

    @Test
    void interruptedHandlerKillsProcess() {
        JavaProcess process = builder().arg("sleep").pipeIO(true).start();
        try {
            assertThrows(
                UncheckedInterruptedException.class, () -> ProcessHandler.defaultHandler().interrupted(process)
            );
        } finally {
            process.killForcibly();
        }
    }

    @Test
    void interruptedHandlerKillsRawProcess() {
        JavaProcess javaProcess = builder().arg("sleep").pipeIO(true).start();
        Process process = javaProcess.getProcess().orElseThrow();
        try {
            assertThrows(
                UncheckedInterruptedException.class, () -> ProcessHandler.defaultHandler().interrupted("id", process)
            );
        } finally {
            process.destroyForcibly();
        }
    }

    private String read(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

}
