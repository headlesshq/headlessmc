package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class VersionJsonServiceImpl implements VersionJsonService {
    private final VersionParser versionParser;
    private final McFilesService mcFilesService;

    /**
     * Creates a service for a single {@code .minecraft} directory.
     *
     * @param versionParser the parser to parse version.json files with.
     * @param mcFiles       the only directory of this service.
     */
    public VersionJsonServiceImpl(VersionParser versionParser, McFiles mcFiles) {
        this(versionParser, McFilesService.of(mcFiles));
    }

    @Override
    public Set<String> getInstalledFileNames() throws HeadlessMcException {
        Set<String> result = new LinkedHashSet<>();
        for (McFiles mcFiles : mcFilesService.all()) {
            result.addAll(getInstalledFileNames(mcFiles));
        }

        return result;
    }

    @Override
    public Set<String> getInstalledFileNames(McFiles mcFiles) throws HeadlessMcException {
        Path versionsDir = mcFiles.getVersionsDir();
        if (!Files.exists(versionsDir)) {
            return Set.of();
        }

        try (Stream<Path> versionDirs = Files.list(versionsDir)) {
            return versionDirs.filter(Files::isDirectory)
                .map(path -> path.resolve(path.getFileName() + ".json"))
                .filter(Files::exists)
                .map(jsonFile -> jsonFile.getParent().getFileName().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to list files in " + versionsDir, e);
        }
    }

    @Override
    public Version getVersion(String fileName) throws HeadlessMcException {
        return tryGetVersion(fileName)
            .orElseThrow(() -> new NotFoundException("Failed to find version " + fileName));
    }

    @Override
    public Optional<Version> tryGetVersion(String fileName) throws HeadlessMcException {
        Optional<Version> result = Optional.empty();
        for (McFiles mcFiles : mcFilesService.all()) {
            Optional<Version> version = tryGetVersion(mcFiles, fileName);
            if (version.isEmpty()) {
                continue;
            }

            if (result.isEmpty()) {
                result = version;
                if (!log.isDebugEnabled()) {
                    break;
                }
            } else {
                log.debug("Version {} in {} is shadowed by the one in {}",
                          fileName, mcFiles.getMcDir(), result.get().asFile().orElse(null));
            }
        }

        return result;
    }

    @Override
    public Optional<Version> tryGetVersion(McFiles mcFiles, String fileName) throws HeadlessMcException {
        Path jsonFile = mcFiles.getVersionsDir().resolve(fileName).resolve(fileName + ".json");
        if (!Files.exists(jsonFile)) {
            return Optional.empty();
        }

        return Optional.of(versionParser.parse(jsonFile));
    }

    @Override
    public ProcessedVersion resolve(Version version) throws HeadlessMcException {
        return resolve(version, MissingVersionInstaller.error());
    }

    @Override
    public ProcessedVersion resolve(Version version, MissingVersionInstaller installer) throws HeadlessMcException {
        // parents are only looked up in the directory of the version, nothing is shared between directories
        McFiles mcFiles = getMcFiles(version);
        List<Version> hierarchy = new LinkedList<>();
        Version currentVersion = version;
        Set<String> checked = new LinkedHashSet<>();
        while (true) {
            if (!checked.add(currentVersion.getInheritsFrom())) {
                throw new NotFoundException("Circular versions: " + checked);
            }

            hierarchy.addFirst(currentVersion);
            if (currentVersion.getInheritsFrom() == null) {
                break;
            }

            Optional<Version> parent = tryGetVersion(mcFiles, currentVersion.getInheritsFrom());
            if (parent.isEmpty()) {
                installer.install(mcFiles, currentVersion.getInheritsFrom());
                parent = tryGetVersion(mcFiles, currentVersion.getInheritsFrom());
                if (parent.isEmpty()) {
                    throw new NotFoundException(
                        "Failed to find version " + currentVersion.getInheritsFrom() + " in " + mcFiles.getMcDir()
                    );
                }
            }

            currentVersion = parent.get();
        }

        return new ProcessedVersion(hierarchy);
    }

    @Override
    public List<Version> getInstalledVersions() throws HeadlessMcException {
        List<Version> versions = new ArrayList<>();
        for (McFiles mcFiles : mcFilesService.all()) {
            versions.addAll(getInstalledVersions(mcFiles));
        }

        return versions;
    }

    @Override
    public List<Version> getInstalledVersions(McFiles mcFiles) throws HeadlessMcException {
        Set<String> fileNames = getInstalledFileNames(mcFiles);
        List<Version> versions = new ArrayList<>(fileNames.size());
        for (String installedVersion : fileNames) {
            try {
                tryGetVersion(mcFiles, installedVersion).ifPresent(versions::add);
            } catch (HeadlessMcException e) {
                log.error("Failed to read version {} in {}", installedVersion, mcFiles.getMcDir(), e);
            }
        }

        return versions;
    }

    @Override
    public McFiles getMcFiles(Version version) {
        return mcFilesService.require(version.asFile().orElse(null));
    }

    @Override
    public VersionParser getParser() {
        return versionParser;
    }

    @Override
    public List<Version> observeChanges(McFiles mcFiles, ChangeAction action) throws HeadlessMcException {
        Set<String> before = getInstalledFileNames(mcFiles);
        action.run();
        Set<String> after = new LinkedHashSet<>(getInstalledFileNames(mcFiles));
        after.removeAll(before);

        List<Version> versions = new ArrayList<>(after.size());
        for (String fileName : after) {
            versions.add(tryGetVersion(mcFiles, fileName)
                .orElseThrow(() -> new NotFoundException("Failed to find version " + fileName)));
        }

        return versions;
    }

    @Override
    public ProcessedVersion process(Version version) throws HeadlessMcException {
        return resolve(version, (mcFiles, id) -> {
            throw new HeadlessMcException(
                "Failed to resolve version " + id + " in " + mcFiles.getMcDir() + " to process " + version.getId()
            );
        });
    }

}
