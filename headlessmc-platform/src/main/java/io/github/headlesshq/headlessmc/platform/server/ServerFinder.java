package io.github.headlesshq.headlessmc.platform.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.nio.file.Path;

/**
 * When a platform supports servers, it needs to provide a way
 * to find the server executable. E.g.:
 * <li> Vanilla: server.jar
 * <li> Paper: paperclip.jar (we are downloading to server.jar) (TODO)
 * <li> Fabric: fabric-server-launch.jar
 * <li> Forge/NeoForge: run.bat/sh files, other .jars
 */
public interface ServerFinder {
    /**
     * The default server.jar.
     */
    String DEFAULT_JAR = "server.jar";

    /**
     * Attempts to find the server executable (jar or script)
     * in the given directory.
     *
     * @param serverDir the directory to search.
     * @return the server executable for the platform.
     * @throws HeadlessMcException if the executable cannot be found.
     */
    Path findExecutable(Path serverDir) throws HeadlessMcException;

}
