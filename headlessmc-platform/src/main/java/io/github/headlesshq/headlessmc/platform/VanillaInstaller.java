package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.version.Version;

/**
 * The {@link VanillaPlatform} is guaranteed to provide
 * a {@link ClientInstaller} and a {@link ServerInstaller}.
 * Many platforms have the installation of the vanilla
 * version as a prerequisite to install their own versions.
 * This interface allows such platforms to simply inject it.
 *
 * @see VanillaPlatform
 */
public interface VanillaInstaller extends ClientInstaller, ServerInstaller {
    /**
     * Retrieves a mc version.json without installing it.
     * Checks first if it is installed locally and otherwise downloads it.
     * This is e.g. useful for servers if they want to find out which
     * Java version to use, or where to download the vanilla server from,
     * without having to install the vanilla client.
     *
     * @param version the version to get the version.json for.
     * @return the parsed version.json as {@link Version}.
     */
    Version getVersion(VanillaVersion version);

}
