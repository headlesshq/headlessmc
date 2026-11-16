package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.Key;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;

import java.net.URI;

/**
 * Installer for client {@link Version}s.
 * if a platform provides {@link ClientSupport},
 * it needs to provide a way to install client versions via this interface.
 * The minimum an installer should do is installing a valid mc version.json
 * file into the versions directory of the given {@link McFiles}.
 */
public interface ClientInstaller {
    Key<Boolean> FORCE_INSTALL = new Key<>(Boolean.class, "hmc.install.args.force.install");
    Key<URI> CUSTOM_INSTALLER_URL = new Key<>(URI.class, "hmc.install.args.custom.installer");

    /**
     * Installs the client into the given {@code .minecraft} directory.
     * There can be multiple {@code .minecraft} directories, an installer must only
     * look at and install into the given one: a version that is already installed in
     * another directory does not count as installed.
     *
     * @param id      the version to install.
     * @param mcFiles the {@code .minecraft} directory to install the client in.
     * @param args    additional arguments to customize the installer.
     * @return the {@link Version} that has been installed, its {@link Version#asFile()} is in the given directory.
     * @throws HeadlessMcException if something goes wrong during Installation.
     */
    Version installClient(VersionID id, McFiles mcFiles, TypedMap args) throws HeadlessMcException;

}
