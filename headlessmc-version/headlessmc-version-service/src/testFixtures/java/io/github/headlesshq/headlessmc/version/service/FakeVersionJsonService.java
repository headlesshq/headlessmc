package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An in-memory {@link VersionJsonService} over a fixed set of versions.
 * It only knows a single {@code .minecraft} directory, all versions are in it,
 * the {@link McFiles} passed to its methods are ignored.
 */
public class FakeVersionJsonService implements VersionJsonService {
    private final Map<String, Version> versions = new LinkedHashMap<>();
    private McFiles mcFiles = McFiles.of(Path.of("fake-mc").toAbsolutePath());

    public FakeVersionJsonService(Version... versions) {
        for (Version version : versions) {
            add(version);
        }
    }

    public FakeVersionJsonService add(Version version) {
        versions.put(version.getId(), version);
        return this;
    }

    /**
     * @param mcFiles the directory this service reports for all versions, see {@link #getMcFiles(Version)}.
     * @return this service.
     */
    public FakeVersionJsonService withMcFiles(McFiles mcFiles) {
        this.mcFiles = mcFiles;
        return this;
    }

    public FakeVersionJsonService remove(String id) {
        versions.remove(id);
        return this;
    }

    @Override
    public Set<String> getInstalledFileNames() {
        return new LinkedHashSet<>(versions.keySet());
    }

    @Override
    public Set<String> getInstalledFileNames(McFiles mcFiles) {
        return getInstalledFileNames();
    }

    @Override
    public Version getVersion(String fileName) throws HeadlessMcException {
        return tryGetVersion(fileName).orElseThrow(
            () -> new HeadlessMcException("Failed to find version " + fileName));
    }

    @Override
    public Optional<Version> tryGetVersion(String fileName) {
        String id = fileName.endsWith(".json")
            ? fileName.substring(0, fileName.length() - ".json".length())
            : fileName;
        return Optional.ofNullable(versions.get(id));
    }

    @Override
    public Optional<Version> tryGetVersion(McFiles mcFiles, String fileName) {
        return tryGetVersion(fileName);
    }

    @Override
    public ProcessedVersion resolve(Version version) throws HeadlessMcException {
        return resolve(version, MissingVersionInstaller.error());
    }

    @Override
    public ProcessedVersion resolve(Version version, MissingVersionInstaller installer) throws HeadlessMcException {
        List<Version> hierarchy = new ArrayList<>();
        Version current = version;
        while (true) {
            hierarchy.addFirst(current);
            String parent = current.getInheritsFrom();
            if (parent == null) {
                break;
            }

            Version parentVersion = versions.get(parent);
            if (parentVersion == null) {
                installer.install(mcFiles, parent);
                parentVersion = versions.get(parent);
                if (parentVersion == null) {
                    throw new HeadlessMcException("Failed to find version " + parent);
                }
            }

            current = parentVersion;
        }

        return new ProcessedVersion(hierarchy);
    }

    @Override
    public List<Version> getInstalledVersions() {
        return new ArrayList<>(versions.values());
    }

    @Override
    public List<Version> getInstalledVersions(McFiles mcFiles) {
        return getInstalledVersions();
    }

    @Override
    public McFiles getMcFiles(Version version) {
        return version.asFile().map(file -> McFilesService.of(mcFiles).require(file)).orElse(mcFiles);
    }

    @Override
    public VersionParser getParser() {
        throw new UnsupportedOperationException("FakeVersionJsonService has no parser");
    }

    @Override
    public List<Version> observeChanges(McFiles mcFiles, ChangeAction action) throws HeadlessMcException {
        Set<String> before = new LinkedHashSet<>(versions.keySet());
        action.run();
        return versions.entrySet().stream()
            .filter(entry -> !before.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();
    }

    @Override
    public ProcessedVersion process(Version version) throws HeadlessMcException {
        return resolve(version);
    }

}
