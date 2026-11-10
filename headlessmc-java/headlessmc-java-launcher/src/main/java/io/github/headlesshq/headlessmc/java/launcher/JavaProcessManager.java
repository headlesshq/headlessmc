package io.github.headlesshq.headlessmc.java.launcher;

import java.nio.file.Path;
import java.util.List;

/**
 * Keeps track of the processes HeadlessMc has launched, e.g. Minecraft clients and servers.
 * Processes are tracked on disk, so that every running HeadlessMc instance can see them.
 */
public interface JavaProcessManager {
    /**
     * Registers a launched process until it exits or is {@link #unregister(RunningProcess) unregistered}.
     *
     * @param id        identifies the process, e.g. the id of the launched version.
     * @param process   the launched process.
     * @param directory the directory the process is running in, e.g. the game directory.
     * @return the registered process.
     */
    RunningProcess register(String id, Process process, Path directory);

    /**
     * Unregisters a process, e.g. after it has exited.
     *
     * @param process the process to unregister.
     */
    void unregister(RunningProcess process);

    /**
     * @return all registered processes that are still running.
     */
    List<RunningProcess> getRunningProcesses();

    /**
     * @param directory the directory to get the running processes for.
     * @return all registered processes that are still running in the given directory.
     */
    default List<RunningProcess> getRunningProcesses(Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        return getRunningProcesses().stream()
            .filter(process -> process.directory().toAbsolutePath().normalize().equals(normalized))
            .toList();
    }

}
