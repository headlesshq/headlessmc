package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VersionJsonService extends VersionChangeService, VersionProcessor {
    Set<String> getInstalledFileNames() throws HeadlessMcException;

    Version getVersion(String fileName) throws HeadlessMcException;

    Optional<Version> tryGetVersion(String fileName) throws HeadlessMcException;

    ProcessedVersion resolve(Version version) throws HeadlessMcException;

    ProcessedVersion resolve(Version version, MissingVersionInstaller installer) throws HeadlessMcException;

    List<Version> getInstalledVersions() throws HeadlessMcException;

    VersionParser getParser();

    @FunctionalInterface
    interface MissingVersionInstaller {
        void install(String versionId) throws HeadlessMcException;

        static MissingVersionInstaller error() {
            return versionId -> {
                throw new HeadlessMcException("Failed to find version " + versionId);
            };
        }
    }

}
