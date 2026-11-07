package io.github.headlesshq.headlessmc.platform.server;

import java.util.List;

/**
 * When installing a server we need to resolve the Java
 * version to launch (and maybe run the platform installer with)
 * for the server.
 *
 * @param javaVersion the java version to launch the server with.
 * @param vmArgs      the default jvm args the server profile should be created with,
 *                    e.g. the flags recommended by Paper.
 */
// TODO: maybe the ServerInstaller should actually get the java version from the outside?
public record ServerInstallation(int javaVersion, List<String> vmArgs) {
    public ServerInstallation(int javaVersion) {
        this(javaVersion, List.of());
    }
}
