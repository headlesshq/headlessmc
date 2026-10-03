package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class VersionJsonServiceImpl implements VersionJsonService {
    private final VersionParser versionParser;
    private final Supplier<Path> versionsDir;

    @Override
    public Set<String> getInstalledFileNames() throws HeadlessMcException {
        Path versionsDir = this.versionsDir.get();
        if (!Files.exists(versionsDir)) {
            return Set.of();
        }

        try (Stream<Path> versionDirs = Files.list(versionsDir)) {
            return versionDirs.filter(Files::isDirectory)
                .map(path -> path.resolve(path.getFileName() + ".json"))
                .filter(Files::exists)
                .map(jsonFile -> jsonFile.getParent().getFileName().toString())
                .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to list files in " + versionParser, e);
        }
    }

    @Override
    public Version getVersion(String fileName) throws HeadlessMcException {
        return tryGetVersion(fileName)
            .orElseThrow(() -> new NotFoundException("Failed to find version " + fileName));
    }

    @Override
    public Optional<Version> tryGetVersion(String fileName) throws HeadlessMcException {
        Path jsonFile = versionsDir.get().resolve(fileName).resolve(fileName + ".json");
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

            Optional<Version> parent = tryGetVersion(currentVersion.getInheritsFrom());
            if (parent.isEmpty()) {
                // TODO: wrap in versionChangeService
                installer.install(currentVersion.getInheritsFrom());
                parent = tryGetVersion(currentVersion.getInheritsFrom());
                if (parent.isEmpty()) {
                    throw new NotFoundException("Failed to find version " + currentVersion.getInheritsFrom());
                }
            }

            currentVersion = parent.get();
        }

        return new ProcessedVersion(hierarchy);
    }

    @Override
    public List<Version> getInstalledVersions() throws HeadlessMcException {
        Set<String> fileNames = getInstalledFileNames();
        List<Version> versions = new ArrayList<>(fileNames.size());
        for (String installedVersion : fileNames) {
            try {
                versions.add(getVersion(installedVersion));
            } catch (HeadlessMcException e) {
                log.error("Failed to read version {}", installedVersion, e);
            }
        }

        return versions;
    }

    @Override
    public VersionParser getParser() {
        return versionParser;
    }

    @Override
    public List<Version> observeChanges(ChangeAction action) throws HeadlessMcException {
        Set<String> before = getInstalledFileNames();
        action.run();
        Set<String> after = new HashSet<>(getInstalledFileNames());
        after.removeAll(before);

        List<Version> versions = new ArrayList<>(after.size());
        for (String fileName : after) {
            versions.add(getVersion(fileName));
        }

        return versions;
    }

    @Override
    public ProcessedVersion process(Version version) throws HeadlessMcException {
        return resolve(version, id -> {
            throw new HeadlessMcException("Failed to resolve version " + id + " to process " + version.getId());
        });
    }

}
