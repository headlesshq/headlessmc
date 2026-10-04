package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.Key;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;

import java.net.URI;
import java.nio.file.Path;

/**
 * Installer for client {@link Version}s.
 * if a platform provides {@link ClientSupport},
 * it needs to provide a way to install client versions via this interface.
 * The minimum an installer should do is installing a valid mc version.json
 * file into the given mcDir/versions.
 */
public interface ClientInstaller {
    Key<Boolean> FORCE_INSTALL = new Key<>(Boolean.class, "hmc.install.args.force.install");
    Key<URI> CUSTOM_INSTALLER_URL = new Key<>(URI.class, "hmc.install.args.custom.installer");

    /**
     * Installs the client into the given mc directory.
     *
     * @param id    the version to install.
     * @param mcDir the mc directory to install the client in.
     * @param args  additional arguments to customize the installer.
     * @return the {@link Version} that has been installed.
     * @throws HeadlessMcException if something goes wrong during Installation.
     */
    Version installClient(VersionID id, Path mcDir, TypedMap args) throws HeadlessMcException;

}
