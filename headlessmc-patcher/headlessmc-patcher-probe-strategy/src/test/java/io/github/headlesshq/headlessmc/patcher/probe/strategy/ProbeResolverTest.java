package io.github.headlesshq.headlessmc.patcher.probe.strategy;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.launcher.AbstractJavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessImpl;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProbeResolverTest {
    /** A process whose stdout is a fixed script and whose stdin is captured. */
    private static final class ScriptedProcess extends Process {
        private final ByteArrayOutputStream stdIn = new ByteArrayOutputStream();
        private final InputStream stdOut;

        private boolean destroyed;

        ScriptedProcess(String script) {
            this.stdOut = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public OutputStream getOutputStream() {
            return stdIn;
        }

        @Override
        public InputStream getInputStream() {
            return stdOut;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }
    }

    @TempDir
    Path root;

    private final List<AbstractJavaProcessBuilder> builders = new ArrayList<>();

    private ScriptedProcess process;

    private JavaLauncherService launcher(Optional<Process> actual) {
        return new JavaLauncherService() {
            @Override
            public JavaProcessBuilder buildProcess() {
                return new AbstractJavaProcessBuilder() {
                    @Override
                    public JavaProcess start() {
                        builders.add(this);
                        return new JavaProcessImpl(
                            getId(), Map.of(), List.of(), List.of(), List.of(),
                            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                            Optional.ofNullable(getJar()), actual
                        );
                    }
                };
            }

            @Override
            public String getName() {
                return "scripted";
            }
        };
    }

    @BeforeEach
    void setup() {
        process = new ScriptedProcess("java/lang/Object\nerror\n");
    }

    private ProbeResolver resolver() {
        return new ProbeResolver(launcher(Optional.of(process)), 21, root.resolve("probe.jar"));
    }

    @Test
    void resolvesTheCommonSuperClass() {
        try (ProbeResolver resolver = resolver()) {
            assertEquals("java/lang/Object", resolver.getCommonSuperClass("a", "b"));
        }

        assertEquals("a" + System.lineSeparator() + "b" + System.lineSeparator(),
            process.stdIn.toString(StandardCharsets.UTF_8));
        AbstractJavaProcessBuilder builder = builders.getFirst();
        assertEquals("probe-super-class", builder.getId());
        assertEquals(root.resolve("probe.jar"), builder.getJar());
        assertEquals(21, builder.getVersion());
        assertTrue(builder.isPipeIO());
    }

    @Test
    void theProcessIsStartedOnlyOnce() {
        try (ProbeResolver resolver = resolver()) {
            resolver.getCommonSuperClass("a", "b");
            assertThrows(PatchException.class, () -> resolver.getCommonSuperClass("c", "d"));
        }

        assertEquals(1, builders.size());
    }

    @Test
    void anErrorFromTheProbeIsReported() {
        try (ProbeResolver resolver = resolver()) {
            resolver.getCommonSuperClass("a", "b");

            // the second scripted answer is "error"
            assertThrows(PatchException.class, () -> resolver.getCommonSuperClass("c", "d"));
        }
    }

    @Test
    void anEndedProbeProcessRestartsTheProcess() {
        ProbeResolver resolver = new ProbeResolver(
            launcher(Optional.of(new ScriptedProcess(""))), 21, root.resolve("probe.jar")
        );

        PatchException e = assertThrows(PatchException.class, () -> resolver.getCommonSuperClass("a", "b"));
        assertTrue(e.getMessage().contains("Failed to get common super class"));

        // the broken process was discarded, so the next call starts a new one
        assertThrows(PatchException.class, () -> resolver.getCommonSuperClass("a", "b"));
        assertEquals(2, builders.size());
        resolver.close();
    }

    @Test
    void aClosedResolverCannotBeUsed() {
        ProbeResolver resolver = resolver();
        resolver.close();

        PatchException e = assertThrows(PatchException.class, () -> resolver.getCommonSuperClass("a", "b"));
        assertTrue(e.getMessage().contains("used after closing"));
    }

    @Test
    void closingAnUnusedResolverIsANoOp() {
        assertDoesNotThrow(() -> resolver().close());
        assertEquals(List.of(), builders);
    }

    @Test
    void closingKillsTheProcess() {
        ProbeResolver resolver = resolver();
        resolver.getCommonSuperClass("a", "b");
        resolver.close();

        assertTrue(process.destroyed);
    }

    @Test
    void virtualProcessesCannotBeProbed() {
        ProbeResolver resolver = new ProbeResolver(
            launcher(Optional.empty()), 21, root.resolve("probe.jar")
        );

        PatchException e = assertThrows(PatchException.class, () -> resolver.getCommonSuperClass("a", "b"));
        assertTrue(e.getMessage().contains("cannot be used for probing"));
    }

    @Test
    void probeProcessesReportIoFailures() {
        JavaProcess javaProcess = launcher(Optional.of(new ScriptedProcess(""))).buildProcess().start();
        try (ProbeProcess probe = ProbeProcess.of(javaProcess)) {
            assertThrows(HeadlessMcIOException.class, () -> probe.getCommonSuperClass("a", "b"));
        }
    }

    @Test
    void theStrategyIsAlwaysApplicable() {
        SuperClassProbeStrategy strategy = new SuperClassProbeStrategy(launcher(Optional.of(process)), null);

        assertTrue(strategy.isApplicable());
        assertEquals(SuperClassStrategy.SORT_PROBE, strategy.sort());
    }

}
