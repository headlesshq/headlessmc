package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the version.json files installed in the {@code .minecraft} directories of the {@link McFilesService}.
 *
 * <p>There can be multiple {@code .minecraft} directories, so methods come in two forms.
 * Methods without {@link McFiles} work on all known directories, the primary directory first.
 * They can return the same version id more than once, and for a name the first directory wins.
 * Methods taking {@link McFiles} work on exactly that directory, use them for everything that
 * installs or removes, where it matters which directory is meant.</p>
 *
 * <p>Every {@link Version} read by this service knows the file it comes from ({@link Version#asFile()}),
 * and with it its directory, see {@link #getMcFiles(Version)}.
 * Parent versions ({@link Version#getInheritsFrom()}) are only looked up in the directory of their child.</p>
 */
public interface VersionJsonService extends VersionChangeService, VersionProcessor {
    /**
     * @return the names of the versions installed in all known directories, without duplicates.
     * @throws HeadlessMcException if a directory cannot be listed.
     */
    Set<String> getInstalledFileNames() throws HeadlessMcException;

    /**
     * @param mcFiles the directory to list.
     * @return the names of the versions installed in the given directory.
     * @throws HeadlessMcException if the directory cannot be listed.
     */
    Set<String> getInstalledFileNames(McFiles mcFiles) throws HeadlessMcException;

    /**
     * @param fileName the name of the version, the name of its directory in the versions directory.
     * @return the version with that name, from the first known directory that has it installed.
     * @throws HeadlessMcException if no such version is installed, or it cannot be read.
     */
    Version getVersion(String fileName) throws HeadlessMcException;

    /**
     * @param fileName the name of the version, the name of its directory in the versions directory.
     * @return the version with that name, from the first known directory that has it installed.
     * @throws HeadlessMcException if the version cannot be read.
     */
    Optional<Version> tryGetVersion(String fileName) throws HeadlessMcException;

    /**
     * @param mcFiles  the directory to look in.
     * @param fileName the name of the version, the name of its directory in the versions directory.
     * @return the version with that name in the given directory.
     * @throws HeadlessMcException if the version cannot be read.
     */
    Optional<Version> tryGetVersion(McFiles mcFiles, String fileName) throws HeadlessMcException;

    /**
     * Resolves the parents of the given version in the directory of the version, see {@link #getMcFiles(Version)}.
     *
     * @param version the version to resolve.
     * @return the version with all its parents.
     * @throws HeadlessMcException if a parent is not installed in the directory of the version.
     */
    ProcessedVersion resolve(Version version) throws HeadlessMcException;

    /**
     * Resolves the parents of the given version in the directory of the version, see {@link #getMcFiles(Version)}.
     *
     * @param version   the version to resolve.
     * @param installer called for a parent that is not installed in the directory of the version.
     * @return the version with all its parents.
     * @throws HeadlessMcException if a parent is not installed and the installer did not install it.
     */
    ProcessedVersion resolve(Version version, MissingVersionInstaller installer) throws HeadlessMcException;

    /**
     * @return the versions installed in all known directories, the versions of the primary directory first.
     * A version that is installed in multiple directories is returned once for each of them,
     * these have different {@link Version#asFile() files}.
     * @throws HeadlessMcException if a directory cannot be listed.
     */
    List<Version> getInstalledVersions() throws HeadlessMcException;

    /**
     * @param mcFiles the directory to list.
     * @return the versions installed in the given directory.
     * @throws HeadlessMcException if the directory cannot be listed.
     */
    List<Version> getInstalledVersions(McFiles mcFiles) throws HeadlessMcException;

    /**
     * The {@code .minecraft} directory everything belonging to a version is found in and installed into:
     * its parents, libraries, assets, logging configs and its jar.
     *
     * @param version the version to get the directory for.
     * @return the directory of the {@link Version#asFile() file} of the version,
     * see {@link McFilesService#require(java.nio.file.Path)},
     * the primary directory for a version that does not come from a file.
     */
    McFiles getMcFiles(Version version);

    VersionParser getParser();

    @FunctionalInterface
    interface MissingVersionInstaller {
        /**
         * @param mcFiles   the directory the version is missing in, and has to be installed into.
         * @param versionId the id of the missing version.
         * @throws HeadlessMcException if the version cannot be installed.
         */
        void install(McFiles mcFiles, String versionId) throws HeadlessMcException;

        static MissingVersionInstaller error() {
            return (mcFiles, versionId) -> {
                throw new HeadlessMcException("Failed to find version " + versionId + " in " + mcFiles.getMcDir());
            };
        }
    }

}
