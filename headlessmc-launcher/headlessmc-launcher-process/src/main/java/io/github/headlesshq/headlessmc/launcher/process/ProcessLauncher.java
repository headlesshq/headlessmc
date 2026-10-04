package io.github.headlesshq.headlessmc.launcher.process;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents a handle that can be used to launch a Mc process.
 * Most importantly, it can be re-used to re-launch a similar process
 * multiple times.
 */
public interface ProcessLauncher {
    /**
     * Launches a mc process.
     *
     * @param pipeIO whether or not to pipe IO, e.g. for tests.
     * @return the launched mc process.
     */
    McProcess launch(boolean pipeIO) throws HeadlessMcException;

    /**
     * Retrieves the builder to build Java processes if it's available.
     * It could e.g. not be available if we are launching the
     * forge/neoforge server via a run.sh/run.bat script.
     *
     * @return the builder used to build the mc process.
     */
    Optional<JavaProcessBuilder> getJavaProcessBuilder();

    // TODO: eventually get rid of this distinction?
    /**
     * Retrieves the builder to build script processes if it's available.
     * This is the case if we are launching the
     * forge/neoforge server via a run.sh/run.bat script.
     *
     * @return the builder used to build the script process.
     */
    Optional<ProcessBuilder> getProcessBuilder();

    /**
     * @return the directory the process will be running in.
     */
    Path getGameDir();

    /**
     * @return the ID the launched process will have.
     */
    String getId();

    static ProcessLauncher of(JavaProcessBuilder javaProcessBuilder, Path gameDir) {
        return new JavaProcessLauncher(javaProcessBuilder, gameDir);
    }

    static ProcessLauncher ofScript(String id, ProcessBuilder processBuilder, Path gameDir) {
        return new ScriptProcessLauncher(processBuilder, id, gameDir);
    }

}
