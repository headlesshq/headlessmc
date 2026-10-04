package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;

/**
 * If a {@link Platform} supports the installation of clients,
 * it needs to implementations for the following classes:
 *
 * @param versionMatcher an interface that allows to map {@link Version}s to {@link VersionID}s.
 * @param installer      an installer that installs the client.
 */
public record ClientSupport(
    VersionMatcher versionMatcher,
    ClientInstaller installer
) {

}
