package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

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
    /**
     * The versions directories to search, versions in earlier directories shadow versions in later ones.
     */
    private final Supplier<? extends List<Path>> versionsDirs;

    @Override
    public Set<String> getInstalledFileNames() throws HeadlessMcException {
        Set<String> result = new LinkedHashSet<>();
        for (Path versionsDir : versionsDirs.get()) {
            result.addAll(getInstalledFileNames(versionsDir));
        }

        return result;
    }

    private Set<String> getInstalledFileNames(Path versionsDir) throws HeadlessMcException {
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
        return tryGetVersion(fileName, null);
    }

    /**
     * @param fileName  the name of the version.json.
     * @param preferred a versions directory to search first, e.g. the one of a child version.
     * @return the parsed version, if one could be found.
     */
    private Optional<Version> tryGetVersion(String fileName, @Nullable Path preferred) throws HeadlessMcException {
        Optional<Path> versionsDir = preferred != null && Files.exists(jsonFile(preferred, fileName))
            ? Optional.of(preferred)
            : getVersionsDir(fileName);
        return versionsDir.map(dir -> versionParser.parse(jsonFile(dir, fileName)));
    }

    @Override
    public Optional<Path> getVersionsDir(String fileName) throws HeadlessMcException {
        List<Path> found = versionsDirs.get()
            .stream()
            .filter(versionsDir -> Files.exists(jsonFile(versionsDir, fileName)))
            .toList();
        if (found.size() > 1) {
            log.debug("Version {} is installed in multiple folders {}, using {}", fileName, found, found.getFirst());
        }

        return found.stream().findFirst();
    }

    private static Path jsonFile(Path versionsDir, String fileName) {
        return versionsDir.resolve(fileName).resolve(fileName + ".json");
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

            // prefer parents in the same folder as their child
            @Nullable Path preferred = getVersionsDir(currentVersion.getId()).orElse(null);
            Optional<Version> parent = tryGetVersion(currentVersion.getInheritsFrom(), preferred);
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
