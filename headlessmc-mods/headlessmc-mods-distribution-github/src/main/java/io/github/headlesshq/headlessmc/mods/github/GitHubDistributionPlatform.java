package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A {@link ModDistributionPlatform} that downloads mods from the releases of the {@link ModRepository}s.
 * By default, the latest version of a mod is used, a specific version can be requested with
 * {@code <mod>@<mod-version>}, e.g. {@code hmc-specifics@3.0.1}.
 * Which release contains a version is decided by {@link ModRepository#getReleaseTag(VersionArg, String)}.
 * The latest versions of the {@link ModRepository#getDependencies()} are always downloaded alongside the mod.
 * <p>A mod is first downloaded into a temporary directory and only then moved into the mods folder.
 * Installed jars that contain the same {@link ModRepository#getModId()} are replaced,
 * so that the mods folder never contains two versions of a mod.
 * If one of them already has the same SHA-256 hash as the release file, nothing is downloaded.
 */
@Slf4j
@GitHub
@ApplicationScoped
public class GitHubDistributionPlatform implements ModDistributionPlatform {
    private static final String VERSION_SEPARATOR = "@";

    private final Instance<ModRepository> repositories;
    private final DownloadService downloadService;
    private final GitHubReleaseService releaseService;
    private final ModCache modCache;
    private final FileService fileService;
    private final ModIdReader modIdReader;

    @Inject
    public GitHubDistributionPlatform(
        @Any Instance<ModRepository> repositories,
        DownloadService downloadService,
        GitHubReleaseService releaseService,
        ModCache modCache,
        FileService fileService,
        ModIdReader modIdReader
    ) {
        this.repositories = repositories;
        this.downloadService = downloadService;
        this.releaseService = releaseService;
        this.modCache = modCache;
        this.fileService = fileService;
        this.modIdReader = modIdReader;
    }

    @Override
    public List<RemoteMod> search(String query, ModType type) throws HeadlessMcException {
        return search(query, type, repository -> true);
    }

    @Override
    public List<RemoteMod> search(String query, VersionArg version, ModType type) throws HeadlessMcException {
        return search(query, type, repository -> repository.supports(version));
    }

    private List<RemoteMod> search(String query, ModType type, Predicate<ModRepository> filter) {
        if (!ModType.MOD.equals(type)) {
            return List.of();
        }

        String lowerCaseQuery = query.toLowerCase(Locale.ENGLISH);
        return repositories.stream()
            .filter(filter)
            .filter(repository -> repository.getName().toLowerCase(Locale.ENGLISH).contains(lowerCaseQuery)
                || repository.getDescription().toLowerCase(Locale.ENGLISH).contains(lowerCaseQuery))
            .map(ModRepository::toMod)
            .toList();
    }

    @Override
    public Path download(
        Path dir,
        @Nullable Path worldDir,
        String id,
        VersionArg version,
        ModType type
    ) throws HeadlessMcException {
        if (!ModType.MOD.equals(type)) {
            throw new ModDistributionException(
                "The " + getName() + " platform only provides mods, but " + type.name() + " was requested for " + id
            );
        }

        String name = id;
        String modVersion = null;
        int versionIndex = id.indexOf(VERSION_SEPARATOR);
        if (versionIndex >= 0) {
            name = id.substring(0, versionIndex);
            modVersion = id.substring(versionIndex + VERSION_SEPARATOR.length());
        }

        ModRepository repository = getRepository(name);
        ResolvedMod mod = resolve(repository, modVersion, version);
        List<ResolvedMod> dependencies = new ArrayList<>();
        for (String dependencyName : repository.getDependencies()) {
            ModRepository dependency = getRepository(dependencyName);
            try {
                dependencies.add(resolve(dependency, null, version));
            } catch (ModDistributionException e) {
                throw new ModDistributionException(
                    "%s requires %s, which is not available for %s".formatted(name, dependencyName, version), e
                );
            }
        }

        try (DownloadContext context = downloadService.context()) {
            for (ResolvedMod dependency : dependencies) {
                install(context, dir, version, dependency);
            }

            return install(context, dir, version, mod);
        }
    }

    private Path install(DownloadContext context, Path dir, VersionArg version, ResolvedMod mod)
        throws HeadlessMcException {
        GitHubRelease.Asset asset = mod.asset();
        Path modsDir = ModType.MOD.directory().getDir(dir);
        Path target = modsDir.resolve(asset.name());
        List<Path> installed = findInstalled(modsDir, mod.repository(), target, version);
        Optional<Path> upToDate = installed.stream().filter(file -> isSameFile(file, asset)).findFirst();
        Path result;
        if (upToDate.isPresent()) {
            result = upToDate.get();
            log.debug("{} is already up to date at {}", mod.repository().getName(), result);
        } else {
            fileService.temp(temp -> {
                Path downloaded = context.download(URI.create(asset.downloadUrl()))
                    .size(asset.size())
                    .sha256(asset.sha256().orElse(null))
                    .toFile(temp.resolve(asset.name()));
                fileService.saveMove(downloaded, target);
            });

            result = target;
        }

        for (Path file : installed) {
            if (!file.equals(result)) {
                log.info("Replacing {} with {}", file.getFileName(), result.getFileName());
                fileService.delete(file);
            }
        }

        modCache.add(result, mod.repository().toMod());
        return result;
    }

    private List<Path> findInstalled(Path modsDir, ModRepository repository, Path target, VersionArg version) {
        if (!Files.isDirectory(modsDir)) {
            return List.of();
        }

        String modId = repository.getModId().orElse(null);
        try (Stream<Path> files = Files.list(modsDir)) {
            return files
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().endsWith(".jar"))
                .filter(file -> file.equals(target)
                    || modId != null && modIdReader.read(file, version).contains(modId))
                .toList();
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to list " + modsDir, e);
        }
    }

    private boolean isSameFile(Path file, GitHubRelease.Asset asset) {
        Optional<String> sha256 = asset.sha256();
        if (sha256.isPresent()) {
            return sha256.get().equalsIgnoreCase(sha256(file));
        }

        try {
            return file.getFileName().toString().equals(asset.name())
                && asset.size() != null
                && Files.size(file) == asset.size();
        } catch (IOException e) {
            log.debug("Failed to get the size of {}", file, e);
            return false;
        }
    }

    private static String sha256(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            log.debug("Failed to hash {}", file, e);
            return "";
        }
    }

    private ResolvedMod resolve(ModRepository repository, @Nullable String modVersion, VersionArg version)
        throws ModDistributionException {
        if (!repository.supports(version)) {
            throw new ModDistributionException(repository.getName() + " does not support " + version);
        }

        GitHubRelease release = releaseService.getRelease(
            repository.getOwner(), repository.getRepository(), repository.getReleaseTag(version, modVersion).orElse(null)
        );
        GitHubRelease.Asset asset = repository.findAsset(release, version)
            .orElseThrow(() -> new ModDistributionException(
                "Release %s of %s/%s contains no file for %s".formatted(
                    release.tagName(), repository.getOwner(), repository.getRepository(), version
                )
            ));

        return new ResolvedMod(repository, asset);
    }

    private ModRepository getRepository(String name) throws ModDistributionException {
        return findRepository(name).orElseThrow(() -> new ModDistributionException(
            "Failed to find mod " + name + " on " + getName() + ", available: "
                + repositories.stream().map(ModRepository::getName).toList()
        ));
    }

    /**
     * Finds the {@link ModRepository} with the given name.
     *
     * @param name the name of the mod ({@link ModRepository#getName()}).
     * @return the repository for the mod with the given name.
     */
    public Optional<ModRepository> findRepository(String name) {
        return repositories.stream()
            .filter(repository -> repository.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    public String getName() {
        return GitHub.PLATFORM_NAME;
    }

    private record ResolvedMod(ModRepository repository, GitHubRelease.Asset asset) {

    }

}
