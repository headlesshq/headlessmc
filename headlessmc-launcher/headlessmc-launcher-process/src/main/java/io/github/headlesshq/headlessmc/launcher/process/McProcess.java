package io.github.headlesshq.headlessmc.launcher.process;

import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;

import java.util.Optional;

/**
 * Represents a launched mc client/server process.
 *
 * @param id          identifies the process for error messages etc.
 * @param javaProcess the process represented as a Java process.
 *                    May not be available, e.g. if the mc server was launched
 *                    via a forge/neoforge run.bat/run.sh script.
 * @param process     the process, ({@link JavaProcess#getProcess()}, or if a script process).
 */
public record McProcess(
    String id,
    Optional<JavaProcess> javaProcess,
    Optional<Process> process
) {
    public int waitFor(ProcessHandler handler) {
        return javaProcess.map(prcs -> prcs.waitFor(handler))
            .or(() -> process.map(prcs -> {
                try {
                    return prcs.waitFor();
                } catch (InterruptedException e) {
                    handler.interrupted(id, prcs);
                    return -Integer.MAX_VALUE;
                }
            })).orElse(0);
    }
}
