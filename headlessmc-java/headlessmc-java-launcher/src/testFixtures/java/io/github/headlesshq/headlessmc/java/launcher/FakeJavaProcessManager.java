package io.github.headlesshq.headlessmc.java.launcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An in-memory {@link JavaProcessManager} that does not need real processes.
 */
public class FakeJavaProcessManager implements JavaProcessManager {
    private final List<RunningProcess> processes = new ArrayList<>();

    /**
     * Registers a running process without a {@link Process}.
     *
     * @param id        identifies the process.
     * @param directory the directory the process is running in.
     * @return the registered process.
     */
    public RunningProcess add(String id, Path directory) {
        RunningProcess process = new RunningProcess(id, processes.size() + 1L, null, directory.toAbsolutePath().normalize());
        processes.add(process);
        return process;
    }

    @Override
    public RunningProcess register(String id, Process process, Path directory) {
        RunningProcess running = new RunningProcess(id, process.pid(), null, directory.toAbsolutePath().normalize());
        processes.add(running);
        return running;
    }

    @Override
    public void unregister(RunningProcess process) {
        processes.remove(process);
    }

    @Override
    public List<RunningProcess> getRunningProcesses() {
        return List.copyOf(processes);
    }

}
