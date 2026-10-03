package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;

import java.time.Duration;

public interface ProcessHandler {
    void handle(String process, int exitCode)
        throws HeadlessMcException;

    void interrupted(JavaProcess process)
        throws HeadlessMcException, UncheckedInterruptedException;

    void interrupted(String id, Process process)
        throws HeadlessMcException, UncheckedInterruptedException;

    // TODO: inject instead
    static ProcessHandler defaultHandler() {
        return new ProcessHandler() {
            @Override
            public void handle(String process, int exitCode) throws HeadlessMcException {
                if (exitCode != 0) {
                    throw new JavaProcessException("Process " + process + " failed with code " + exitCode);
                }
            }

            @Override
            public void interrupted(JavaProcess process) {
                try {
                    process.kill();
                    int result = process.waitFor(Duration.ofSeconds(30L));
                    throw new UncheckedInterruptedException(
                        "Process " + process.getId() + " was killed with exit code " + result
                    );
                } catch (HeadlessMcException | UncheckedInterruptedException e) {
                    process.killForcibly();
                    throw new UncheckedInterruptedException(e);
                }
            }

            @Override
            public void interrupted(String id, Process process) throws HeadlessMcException, UncheckedInterruptedException {
                try {
                    process.destroy();
                    Duration duration = Duration.ofSeconds(30L);
                    if (process.waitFor(duration)) {
                        throw new UncheckedInterruptedException(
                            "Process " + id + " was killed with exit code " + process.exitValue()
                        );
                    } else {
                        throw new JavaProcessException(
                            "Timeout while waiting " + duration + " for process"
                        );
                    }
                } catch (InterruptedException | HeadlessMcException | UncheckedInterruptedException e) {
                    process.destroyForcibly();
                    throw new UncheckedInterruptedException(e);
                }
            }
        };
    }

}
