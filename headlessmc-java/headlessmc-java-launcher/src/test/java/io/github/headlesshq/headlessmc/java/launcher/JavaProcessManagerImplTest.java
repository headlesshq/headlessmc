package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaProcessManagerImplTest {
    @TempDir
    Path root;

    private JavaProcessManagerImpl manager() {
        return new JavaProcessManagerImpl(new DefaultJacksonJsonService(), TestFiles.appFiles(root));
    }

    private Path processDir() {
        return TestFiles.appFiles(root).getStateDir().resolve("processes");
    }

    @Test
    void registersRunningProcesses() {
        JavaProcessManagerImpl manager = manager();
        Process current = currentProcess();
        RunningProcess running = manager.register("test", current, root.resolve("game"));

        assertEquals(List.of(running), manager.getRunningProcesses());
        assertEquals(List.of(running), manager.getRunningProcesses(root.resolve("game").resolve(".")));
        assertEquals(List.of(), manager.getRunningProcesses(root.resolve("other")));
        assertEquals(List.of(running), manager().getRunningProcesses());

        manager.unregister(running);
        assertEquals(List.of(), manager.getRunningProcesses());
    }

    @Test
    void deletesProcessesThatAreNotAlive() throws IOException {
        Files.createDirectories(processDir());
        Files.writeString(
            processDir().resolve(Long.MAX_VALUE + ".json"),
            "{\"id\": \"dead\", \"pid\": " + Long.MAX_VALUE + ", \"directory\": \"" + root.toString().replace("\\", "\\\\") + "\"}"
        );

        assertEquals(List.of(), manager().getRunningProcesses());
        assertFalse(Files.exists(processDir().resolve(Long.MAX_VALUE + ".json")));
    }

    @Test
    void deletesProcessesWithReusedPid() {
        JavaProcessManagerImpl manager = manager();
        RunningProcess running = manager.register("test", currentProcess(), root);
        new DefaultJacksonJsonService().write(
            processDir().resolve(running.pid() + ".json"),
            new RunningProcess("test", running.pid(), Instant.EPOCH, root),
            true
        );

        assertEquals(List.of(), manager.getRunningProcesses());
    }

    @Test
    void ignoresBrokenFiles() throws IOException {
        Files.createDirectories(processDir());
        Files.writeString(processDir().resolve("1.json"), "not json");

        assertEquals(List.of(), manager().getRunningProcesses());
    }

    private Process currentProcess() {
        ProcessHandle handle = ProcessHandle.current();
        return new Process() {
            @Override
            public OutputStream getOutputStream() {
                return OutputStream.nullOutputStream();
            }

            @Override
            public InputStream getInputStream() {
                return InputStream.nullInputStream();
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
            }

            @Override
            public long pid() {
                return handle.pid();
            }

            @Override
            public ProcessHandle.Info info() {
                return handle.info();
            }
        };
    }

}
