package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
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
 */
public class FakeVersionJsonService implements VersionJsonService {
    private final Map<String, Version> versions = new LinkedHashMap<>();
    private final Map<String, Path> versionsDirs = new LinkedHashMap<>();

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
     * Adds a version that is located in the given versions directory, see {@link #getVersionsDir(String)}.
     */
    public FakeVersionJsonService add(Version version, Path versionsDir) {
        versionsDirs.put(version.getId(), versionsDir);
        return add(version);
    }

    public FakeVersionJsonService remove(String id) {
        versions.remove(id);
        versionsDirs.remove(id);
        return this;
    }

    @Override
    public Set<String> getInstalledFileNames() {
        return new LinkedHashSet<>(versions.keySet());
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
    public Optional<Path> getVersionsDir(String fileName) {
        return Optional.ofNullable(versionsDirs.get(fileName));
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
                installer.install(parent);
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
    public VersionParser getParser() {
        throw new UnsupportedOperationException("FakeVersionJsonService has no parser");
    }

    @Override
    public List<Version> observeChanges(ChangeAction action) throws HeadlessMcException {
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
