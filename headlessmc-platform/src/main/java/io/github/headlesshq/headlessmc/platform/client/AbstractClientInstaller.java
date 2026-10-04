package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.Vanilla;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Helper base class for {@link ClientInstaller}s.
 * For most mod platforms (fabric, forge, neoforge),
 * the installation of the vanilla client is required
 * beforehand.
 * We also need the vanilla version to get the
 * java version to install/launch the client with.
 * This class helps with that.
 */
@RequiredArgsConstructor
public abstract class AbstractClientInstaller implements ClientInstaller {
    /**
     * The actual installation method for this {@link ClientInstaller}.
     * It is called after it has been ensured that the vanilla
     * version has been installed.
     *
     * @param id      the version to install the client for.
     * @param mcDir   the mc directory to install the client in.
     * @param vanilla the vanilla parent version for the version to install.
     * @param args    special arguments for the client.
     * @throws HeadlessMcException if something goes wrong.
     */
    protected abstract void installClient(VersionID id, Path mcDir, Version vanilla, TypedMap args)
        throws HeadlessMcException;

    /**
     * Provides common services needed for the installation of
     * the (vanilla) client.
     *
     * <br>This is not a field, because otherwise this abstract super class
     * would make implementations un-proxyable, and not suited for
     * being {@link ApplicationScoped}.
     *
     * @return common services needed for the installation of
     * the (vanilla) client.
     */
    protected abstract InstallerServices getServices();

    /**
     * @return the name of the platform this installer installs clients for.
     */
    protected abstract String getPlatformName();

    @Override
    public Version installClient(
        VersionID id,
        Path mcDir,
        TypedMap args
    ) throws HeadlessMcException {
        if (!getPlatformName().equals(id.getPlatform().getName())) {
            throw new IllegalArgumentException(
                "Trying to install version %s with installer for platform %s".formatted(id, getPlatformName())
            );
        }

        VersionJsonService versionService = getServices().getVersionService();
        VersionMatcherService versionMatcherService = getServices().getVersionMatcherService();

        List<Version> installed = versionService.getInstalledVersions();
        if (!args.get(ClientInstaller.FORCE_INSTALL, false)) {
            Optional<VersionMatcherService.MatchResult> match =
                versionMatcherService.match(id, installed, versionService);

            if (match.isPresent()) {
                return match.get().version();
            }
        }

        Version vanilla = installVanillaVersion(id, mcDir, args);
        List<Version> versions = versionService.observeChanges(() -> installClient(id, mcDir, vanilla, args));

        return versionMatcherService.match(id, versions, versionService)
            .orElseThrow(() -> new VersionMatchException("Failed to find install version " + id + " in " + versions))
            .version();
    }

    protected Version installVanillaVersion(VersionID id, Path mcDir, TypedMap args)
        throws HeadlessMcException {
        return getServices().getVanillaInstaller().installClient(id.asVanillaVersion(), mcDir, args);
    }

    /**
     * Common services needed by an {@link AbstractClientInstaller}.
     * We do not declare any fields, because otherwise we cannot
     * have {@link ApplicationScoped} implementations of this class.
     * This way we can inject the {@code CommonInstallerServices}.
     */
    public interface InstallerServices {
        /**
         * @return a service to use to map the installed Version to a VersionID.
         */
        VersionMatcherService getVersionMatcherService();

        /**
         * @return a server to use to parse a Mc version.json files.
         */
        VersionJsonService getVersionService();

        /**
         * @return the installer for the {@link Vanilla} platform.
         */
        VanillaInstaller getVanillaInstaller();
    }

}
