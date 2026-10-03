package io.github.headlesshq.headlessmc.platform.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;

import java.nio.file.Path;

/**
 * When a {@link io.github.headlesshq.headlessmc.platform.Platform}
 * provides server support it needs to provide a way to install
 * the server.
 */
public interface ServerInstaller {
    /**
     * Installs the server in the given directory.
     *
     * @param id   the version to install the server for.
     * @param dir  the directory to install the server in.
     * @param args special arguments for the platform installer (e.g. display UI)
     * @return an {@link Installation} with the java version that was used to
     * install (and should be used to launch) the server.
     * @throws HeadlessMcException if something goes wrong.
     */
    Installation installServer(VersionID id, Path dir, TypedMap args) throws HeadlessMcException;

    /**
     * When installing a server we need to resolve the Java
     * version to launch (and maybe run the platform installer with)
     * for the server.
     *
     * @param javaVersion the java version to launch the server with.
     */
    // TODO: maybe the ServerInstaller should actually get the java version from the outside?
    record Installation(int javaVersion) {}

}
