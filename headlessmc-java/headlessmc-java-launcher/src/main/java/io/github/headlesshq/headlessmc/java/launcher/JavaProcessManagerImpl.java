package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Registers every process as a json file named after its pid in the {@code processes} directory
 * of the {@link AppFiles#getStateDir() state directory}.
 * Files of processes that are no longer alive, or whose pid has been reused by another process,
 * are deleted when the running processes are listed.
 * Files are written to a temporary file and moved into place, so that other instances never read a half-written file.
 * Files that cannot be read are only deleted if the process with the pid of their name is not alive.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaProcessManagerImpl implements JavaProcessManager {
    private static final String JSON = ".json";

    private final JsonService jsonService;
    private final AppFiles appFiles;

    @Override
    public RunningProcess register(String id, Process process, Path directory) {
        Instant started = process.info().startInstant().map(this::truncate).orElse(null);
        RunningProcess running = new RunningProcess(id, process.pid(), started, directory.toAbsolutePath().normalize());
        Path file = getFile(running.pid());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            jsonService.write(temp, running, true);
            move(temp, file);
        } catch (JsonParseException | IOException e) {
            log.warn("Failed to register process {} ({})", id, running.pid(), e);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
        }

        return running;
    }

    @Override
    public void unregister(RunningProcess process) {
        try {
            Files.deleteIfExists(getFile(process.pid()));
        } catch (IOException e) {
            log.warn("Failed to unregister process {} ({})", process.id(), process.pid(), e);
        }
    }

    @Override
    public List<RunningProcess> getRunningProcesses() {
        Path dir = getProcessDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        List<RunningProcess> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(file -> file.getFileName().toString().endsWith(JSON)).toList()) {
                Optional<RunningProcess> process = read(file);
                if (process.isPresent()) {
                    process.filter(this::isAlive).ifPresentOrElse(result::add, () -> deleteStale(file));
                } else if (!isPidAlive(file)) {
                    deleteStale(file);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to list running processes in {}", dir, e);
        }

        return result;
    }

    /**
     * @param file a process file that could not be read.
     * @return {@code false} if the name of the file is a pid, and no process with that pid is alive.
     */
    private boolean isPidAlive(Path file) {
        String name = file.getFileName().toString();
        try {
            long pid = Long.parseLong(name.substring(0, name.length() - JSON.length()));
            return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isAlive(RunningProcess process) {
        Optional<ProcessHandle> handle = ProcessHandle.of(process.pid());
        if (handle.isEmpty() || !handle.get().isAlive()) {
            return false;
        }

        Instant started = process.started();
        return started == null || handle.get().info().startInstant()
            .map(this::truncate)
            .map(started::equals)
            .orElse(true);
    }

    private Optional<RunningProcess> read(Path file) {
        try {
            return Optional.of(jsonService.parse(file, RunningProcess.class));
        } catch (JsonParseException e) {
            log.warn("Failed to read running process {}", file, e);
            return Optional.empty();
        }
    }

    private void deleteStale(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete stale process file {}", file, e);
        }
    }

    private Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS);
    }

    private Path getFile(long pid) {
        return getProcessDir().resolve(pid + JSON);
    }

    private Path getProcessDir() {
        return appFiles.getStateDir().resolve("processes");
    }

}
