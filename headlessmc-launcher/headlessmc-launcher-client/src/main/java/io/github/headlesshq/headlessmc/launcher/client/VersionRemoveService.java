package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.assets.AssetIndex;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Removes installed version.jsons together with the libraries, assets and logging configs
 * that no other installed version needs.
 * Everything that any other installed version, or a version in its {@link Version#getInheritsFrom()} hierarchy,
 * references is kept.
 * Versions are launched with the files of the .minecraft folder they are installed in,
 * so files are kept if any other version in the same folder needs them.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionRemoveService {
    private final VersionMatcherService versionMatcherService;
    private final VersionJsonService versionJsonService;
    private final PlatformService platformService;
    private final ProfileService profileService;
    private final FileService fileService;
    private final VersionMcFilesService versionMcFilesService;
    private final McFilesService mcFilesService;
    private final JsonService jsonService;

    /**
     * Finds the installed version to remove.
     *
     * @param args either the id of an installed version.json, e.g. {@code fabric-loader-0.16.9-1.21.1},
     *             or a {@link VersionArg}, e.g. {@code fabric 1.21.1 0.16.9}.
     * @return the single installed version the args refer to.
     * @throws IllegalArgumentException if no or more than one installed version matches the args.
     */
    public Version findVersion(List<String> args) throws HeadlessMcException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException(
                "Please specify the version to remove, e.g. 'fabric 1.21.1 0.16.9' or the name of a version.json."
            );
        }

        List<Version> installed = versionJsonService.getInstalledVersions();
        if (args.size() == 1) {
            Optional<Version> byId = installed.stream().filter(v -> v.getId().equals(args.getFirst())).findFirst();
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        VersionID id = VersionID.resolve(platformService, VersionArg.parse(args));
        Map<String, Set<VersionID>> ids = matchAll(installed);
        List<Version> matches = installed.stream().filter(version -> matches(ids.get(version.getId()), id)).toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Failed to find an installed version for " + id);
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException("Found multiple installed versions for %s: %s, please specify the build or the name of the version.json.".formatted(
                id, matches.stream().map(Version::getId).sorted().toList()
            ));
        }

        return matches.getFirst();
    }

    /**
     * Determines what removing the given version would delete and what it would break.
     *
     * @param version the installed version to remove.
     * @return the removal, which can be passed to {@link #remove(VersionRemoval)}.
     */
    public VersionRemoval plan(Version version) throws HeadlessMcException {
        return plan(version, false);
    }

    /**
     * Determines what removing the given version would delete and what it would break.
     *
     * @param version        the installed version to remove.
     * @param withDependents also removes the versions inheriting from the version
     *                       and the profiles that could no longer be launched.
     * @return the removal, which can be passed to {@link #remove(VersionRemoval)}.
     */
    public VersionRemoval plan(Version version, boolean withDependents) throws HeadlessMcException {
        Map<String, Version> installed = new LinkedHashMap<>();
        versionJsonService.getInstalledVersions().forEach(v -> installed.put(v.getId(), v));
        installed.put(version.getId(), version);

        List<Version> dependents = installed.values().stream()
            .filter(other -> !other.getId().equals(version.getId()))
            .filter(other -> hierarchy(other, installed).stream().anyMatch(v -> v.getId().equals(version.getId())))
            .toList();
        List<Version> removedVersions = new ArrayList<>();
        removedVersions.add(version);
        if (withDependents) {
            removedVersions.addAll(dependents);
        }

        Map<String, McFiles> folders = new HashMap<>();
        for (Version installedVersion : installed.values()) {
            folders.put(installedVersion.getId(), versionMcFilesService.getMcFiles(installedVersion));
        }

        Set<String> removedIds = removedVersions.stream().map(Version::getId).collect(Collectors.toSet());
        List<List<Version>> needed = installed.values().stream()
            .filter(other -> !removedIds.contains(other.getId()))
            .map(other -> hierarchy(other, installed))
            .toList();

        Set<Path> files = new LinkedHashSet<>();
        for (Version removedVersion : removedVersions) {
            List<Version> removed = hierarchy(removedVersion, installed);
            McFiles mcFiles = folders.get(removedVersion.getId());
            // versions launch with the files of their own .minecraft folder, not the ones of their parents
            List<List<Version>> neededInFolder = needed.stream()
                .filter(hierarchy -> mcFiles.equals(folders.get(hierarchy.getLast().getId())))
                .toList();
            files.addAll(unusedLibraries(mcFiles, removed, neededInFolder));
            files.addAll(unusedAssets(mcFiles, removed, neededInFolder));
            files.addAll(unusedLoggingConfigs(mcFiles, removed, neededInFolder));
        }

        for (Version removedVersion : removedVersions.reversed()) {
            McFiles mcFiles = folders.get(removedVersion.getId());
            files.add(fileService.getPath(mcFiles.getVersionsDir(), removedVersion.getId()));
        }

        List<Path> mcDirs = mcFilesService.getAllMcFiles().stream()
            .map(mcFiles -> mcFiles.getMcDir().toAbsolutePath().normalize())
            .toList();
        List<Path> existing = files.stream()
            .filter(file -> mcDirs.stream().anyMatch(file.toAbsolutePath().normalize()::startsWith))
            .filter(Files::exists)
            .toList();

        List<Profile> blockingProfiles = blockingProfiles(dependents, version, installed);
        return new VersionRemoval(
            version,
            dependents,
            blockingProfiles,
            List.copyOf(removedVersions),
            withDependents ? blockingProfiles : List.of(),
            existing
        );
    }

    /**
     * Deletes the files and removes the profiles of the given removal, regardless of {@link VersionRemoval#isBlocked()}.
     *
     * @param removal the removal created by {@link #plan(Version, boolean)}.
     */
    public void remove(VersionRemoval removal) throws HeadlessMcException {
        for (Profile profile : removal.removedProfiles()) {
            profileService.remove(profile);
        }

        for (Path file : removal.files()) {
            fileService.delete(file);
            deleteEmptyParents(file);
        }

        log.info("Removed versions {} and profiles {}, deleted {} files",
            removal.removedVersions().stream().map(Version::getId).toList(),
            removal.removedProfiles().stream().map(Profile::name).toList(),
            removal.files().size());
    }

    /**
     * Matches every version only once, matching versions no platform understands can be expensive.
     */
    private Map<String, Set<VersionID>> matchAll(Collection<Version> versions) {
        Map<String, Set<VersionID>> result = new HashMap<>();
        for (Version version : versions) {
            try {
                result.put(version.getId(), versionMatcherService.match(version, versionJsonService));
            } catch (HeadlessMcException e) {
                log.debug("Failed to match version {}", version.getId(), e);
                result.put(version.getId(), Set.of());
            }
        }

        return result;
    }

    /**
     * Like {@link VersionMatcherService#match(VersionID, Collection, VersionProcessor)},
     * a VersionID for any build matches every build, a VersionID with a build only that build.
     */
    private static boolean matches(Set<VersionID> matches, VersionID id) {
        return matches.stream().anyMatch(match ->
            match.isSamePlatformVersion(id) && (id.isAnyBuild() || match.getBuild().equals(id.getBuild()))
        );
    }

    /**
     * Profiles launch the first installed version matching their {@link Profile#currentVersion()}.
     * A profile is blocking if it finds a version now, but would not find one after the removal.
     * Profiles on the {@code latest} build are treated like profiles for any build.
     */
    private List<Profile> blockingProfiles(List<Version> dependents, Version version, Map<String, Version> installed) {
        Set<String> removedIds = new HashSet<>();
        removedIds.add(version.getId());
        dependents.forEach(dependent -> removedIds.add(dependent.getId()));
        Map<String, Set<VersionID>> ids = matchAll(installed.values());

        List<Profile> result = new ArrayList<>();
        for (Profile profile : profileService.getProfiles()) {
            if (!Side.CLIENT.equals(profile.side())) {
                continue;
            }

            VersionArg arg = profile.currentVersion();
            if (arg.isLatest()) {
                arg = arg.withBuild(null);
            }

            try {
                VersionID id = VersionID.resolve(platformService, arg);
                boolean foundBefore = ids.values().stream().anyMatch(matches -> matches(matches, id));
                boolean foundAfter = ids.entrySet().stream()
                    .filter(entry -> !removedIds.contains(entry.getKey()))
                    .anyMatch(entry -> matches(entry.getValue(), id));
                if (foundBefore && !foundAfter) {
                    result.add(profile);
                }
            } catch (HeadlessMcException | IllegalArgumentException e) {
                log.warn("Failed to check if profile {} needs version {}", profile.name(), version.getId(), e);
            }
        }

        return result;
    }

    private List<Path> unusedLibraries(McFiles mcFiles, List<Version> removed, List<List<Version>> needed) {
        Set<Path> neededLibraries = needed.stream()
            .flatMap(List::stream)
            .flatMap(version -> libraryPaths(mcFiles, version).stream())
            .collect(Collectors.toSet());

        return removed.stream()
            .flatMap(version -> libraryPaths(mcFiles, version).stream())
            .filter(path -> !neededLibraries.contains(path))
            .toList();
    }

    /**
     * All paths in the given .minecraft folder a library of the version could have been downloaded to, for any OS.
     */
    private Set<Path> libraryPaths(McFiles mcFiles, Version version) {
        Set<Path> paths = new LinkedHashSet<>();
        for (Version.Library library : version.getLibraries()) {
            try {
                paths.add(library.getArtifact().jar(mcFiles.getLibraryDir()).toAbsolutePath().normalize());
            } catch (IllegalArgumentException e) {
                log.warn("Failed to parse library {} of version {}", library.getName(), version.getId(), e);
            }

            Version.LibraryDownloads downloads = library.getDownloads();
            if (downloads != null) {
                addLibraryPath(mcFiles, paths, downloads.getArtifact());
                Map<String, Version.Download> classifiers = downloads.getClassifiers();
                if (classifiers != null) {
                    classifiers.values().forEach(download -> addLibraryPath(mcFiles, paths, download));
                }
            }
        }

        return paths;
    }

    private void addLibraryPath(McFiles mcFiles, Set<Path> paths, Version.@Nullable Download download) {
        String path = download == null ? null : download.getPath();
        if (path != null) {
            try {
                paths.add(fileService.getPath(mcFiles.getLibraryDir(), path.split("/")).toAbsolutePath().normalize());
            } catch (HeadlessMcException e) {
                log.warn("Invalid library path {}", path, e);
            }
        }
    }

    /**
     * If no other version uses the asset index of the removed version, the index,
     * its virtual directory, the asset objects and the files it mapped to the resources directory are unused,
     * unless they are referenced by the asset index of another version.
     */
    private List<Path> unusedAssets(McFiles mcFiles, List<Version> removed, List<List<Version>> needed) {
        String id = assetIndexId(removed).orElse(null);
        if (id == null) {
            return List.of();
        }

        Set<String> neededIds = needed.stream()
            .map(this::assetIndexId)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
        if (neededIds.contains(id)) {
            return List.of();
        }

        List<Path> result = new ArrayList<>();
        result.add(fileService.getPath(mcFiles.getAssetsDir(), "virtual", id));
        Path indexFile = indexFile(mcFiles, id);
        result.add(indexFile);
        Optional<AssetIndex> index = readAssetIndex(indexFile);
        if (index.isEmpty()) {
            return result;
        }

        Set<String> neededHashes = new HashSet<>();
        Set<String> neededResources = new HashSet<>();
        for (String neededId : neededIds) {
            Path neededIndexFile = indexFile(mcFiles, neededId);
            if (!Files.exists(neededIndexFile)) {
                continue;
            }

            Optional<AssetIndex> neededIndex = readAssetIndex(neededIndexFile);
            if (neededIndex.isEmpty()) {
                log.warn("Keeping all asset objects of {}, failed to read needed asset index {}", id, neededIndexFile);
                return result;
            }

            neededIndex.get().objects().values().forEach(download -> neededHashes.add(download.hash()));
            if (neededIndex.get().mapToResources()) {
                neededResources.addAll(neededIndex.get().objects().keySet());
            }
        }

        for (Map.Entry<String, AssetIndex.Download> entry : index.get().objects().entrySet()) {
            String hash = entry.getValue().hash();
            if (!neededHashes.contains(hash)) {
                result.add(fileService.getPath(mcFiles.getAssetsDir(), "objects", hash.substring(0, 2), hash));
            }

            if (index.get().mapToResources() && !neededResources.contains(entry.getKey())) {
                result.add(fileService.getPath(mcFiles.getResourcesDir(), entry.getKey()));
            }
        }

        return result;
    }

    private Path indexFile(McFiles mcFiles, String id) {
        return fileService.getPath(mcFiles.getAssetsDir(), "indexes", id + ".json");
    }

    private Optional<AssetIndex> readAssetIndex(Path file) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonService.parse(file, AssetIndex.class));
        } catch (HeadlessMcException e) {
            log.warn("Failed to read asset index {}", file, e);
            return Optional.empty();
        }
    }

    private Optional<String> assetIndexId(List<Version> hierarchy) {
        return hierarchy.reversed().stream()
            .map(Version::getAssetIndex)
            .filter(Objects::nonNull)
            .findFirst()
            .map(Version.AssetIndex::getId);
    }

    private List<Path> unusedLoggingConfigs(McFiles mcFiles, List<Version> removed, List<List<Version>> needed) {
        Set<String> neededIds = needed.stream()
            .map(this::loggingConfigId)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());

        return loggingConfigId(removed)
            .filter(id -> !neededIds.contains(id))
            .map(id -> {
                Path file = fileService.getPath(mcFiles.getAssetsDir(), "log_configs", id);
                return List.of(file, file.resolveSibling(id + "-patched.xml"));
            }).orElse(List.of());
    }

    private Optional<String> loggingConfigId(List<Version> hierarchy) {
        return hierarchy.reversed().stream()
            .map(Version::getLogging)
            .filter(Objects::nonNull)
            .map(logging -> logging.get(Version.LOGGING_CLIENT))
            .filter(Objects::nonNull)
            .findFirst()
            .map(configuration -> configuration.getFile().getId());
    }

    /**
     * The installed versions in the {@link Version#getInheritsFrom()} hierarchy of the version,
     * starting with the oldest parent and ending with the version.
     */
    private List<Version> hierarchy(Version version, Map<String, Version> installed) {
        List<Version> hierarchy = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        @Nullable Version current = version;
        while (current != null && visited.add(current.getId())) {
            hierarchy.addFirst(current);
            String parent = current.getInheritsFrom();
            current = parent == null ? null : installed.get(parent);
        }

        return hierarchy;
    }

    private void deleteEmptyParents(Path file) {
        List<Path> roots = mcFilesService.getAllMcFiles()
            .stream()
            .flatMap(mcFiles -> Stream.of(mcFiles.getLibraryDir(), mcFiles.getAssetsDir(), mcFiles.getResourcesDir()))
            .map(root -> root.toAbsolutePath().normalize())
            .toList();
        Path parent = file.toAbsolutePath().normalize().getParent();
        while (parent != null && isBelowAny(parent, roots) && isEmptyDirectory(parent)) {
            fileService.delete(parent);
            parent = parent.getParent();
        }
    }

    private boolean isBelowAny(Path path, List<Path> roots) {
        return roots.stream().anyMatch(root -> path.startsWith(root) && !path.equals(root));
    }

    private boolean isEmptyDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }

        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findFirst().isEmpty();
        } catch (IOException e) {
            log.warn("Failed to check if {} is empty", dir, e);
            return false;
        }
    }

}
