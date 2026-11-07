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
     * @return an {@link ServerInstallation} with the java version that was used to
     * install (and should be used to launch) the server and the default jvm args for it.
     * @throws HeadlessMcException if something goes wrong.
     */
    ServerInstallation installServer(VersionID id, Path dir, TypedMap args) throws HeadlessMcException;
}
