package io.github.headlesshq.headlessmc.platform.server;

import io.github.headlesshq.headlessmc.platform.Platform;

/**
 * If a {@link Platform} supports the installation of Servers,
 * it needs to implementations for the following classes:
 *
 * @param serverFinder an interface that supports finding the executable to run a server.
 * @param installer    an installer that installs servers.
 */
public record ServerSupport(
    ServerFinder serverFinder, ServerInstaller installer
) {

}
