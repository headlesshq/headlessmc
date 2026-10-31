package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

/**
 * Finds the .minecraft folder of installed versions, as versions can be installed in any of the folders
 * provided by the {@link McFilesService}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionMcFilesService {
    private final VersionJsonService versionJsonService;
    private final McFilesService mcFilesService;

    /**
     * @param version the version to get the .minecraft folder for.
     * @return the .minecraft folder the version is installed in, or the primary one if it is not installed.
     * @throws HeadlessMcException if the versions directories cannot be read.
     */
    public McFiles getMcFiles(Version version) throws HeadlessMcException {
        return getMcFiles(version.getId());
    }

    /**
     * @param fileName the name of the version.json.
     * @return the .minecraft folder the version.json is installed in, or the primary one if it is not installed.
     * @throws HeadlessMcException if the versions directories cannot be read.
     */
    public McFiles getMcFiles(String fileName) throws HeadlessMcException {
        return versionJsonService.getVersionsDir(fileName)
            .flatMap(mcFilesService::getMcFiles)
            .orElseGet(mcFilesService::getPrimaryMcFiles);
    }

    /**
     * Mod-loading platforms install into the folder of their vanilla version, as their installers need it.
     *
     * @param id the version to install.
     * @return the .minecraft folder the vanilla version of the id is installed in,
     * or the primary one if it is not installed.
     * @throws HeadlessMcException if the versions directories cannot be read.
     */
    public McFiles getInstallMcFiles(VersionID id) throws HeadlessMcException {
        return getMcFiles(id.getVersion().getName());
    }

}
