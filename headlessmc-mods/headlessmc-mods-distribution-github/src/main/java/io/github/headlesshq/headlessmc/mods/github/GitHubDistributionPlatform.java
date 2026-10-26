package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@link ModDistributionPlatform} that downloads mods from the releases of the {@link ModRepository}s.
 * By default, the latest version of a mod is used, a specific version can be requested with
 * {@code <mod>@<mod-version>}, e.g. {@code hmc-specifics@3.0.1}.
 * Which release contains a version is decided by {@link ModRepository#getReleaseTag(VersionArg, String)}.
 * The latest versions of the {@link ModRepository#getDependencies()} are always downloaded alongside the mod.
 */
@GitHub
@ApplicationScoped
public class GitHubDistributionPlatform implements ModDistributionPlatform {
    private static final String VERSION_SEPARATOR = "@";

    private final Instance<ModRepository> repositories;
    private final DownloadService downloadService;
    private final GitHubAPI api;
    private final ModCache modCache;

    @Inject
    public GitHubDistributionPlatform(
        @Any Instance<ModRepository> repositories,
        DownloadService downloadService,
        @RestClient GitHubAPI api,
        ModCache modCache
    ) {
        this.repositories = repositories;
        this.downloadService = downloadService;
        this.api = api;
        this.modCache = modCache;
    }

    @Override
    public List<RemoteMod> search(String query, Set<ModType> types) throws HeadlessMcException {
        return search(query, types, repository -> true);
    }

    @Override
    public List<RemoteMod> search(String query, VersionArg version, Set<ModType> types) throws HeadlessMcException {
        return search(query, types, repository -> repository.supports(version));
    }

    private List<RemoteMod> search(String query, Set<ModType> types, Predicate<ModRepository> filter) {
        if (!types.isEmpty() && !types.contains(ModType.MOD)) {
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
                download(context, dir, dependency);
            }

            return download(context, dir, mod);
        }
    }

    private Path download(DownloadContext context, Path dir, ResolvedMod mod) throws HeadlessMcException {
        GitHubRelease.Asset asset = mod.asset();
        Path result = context.download(URI.create(asset.downloadUrl()))
            .size(asset.size())
            .sha256(asset.sha256().orElse(null))
            .toFile(ModType.MOD.directory().getDir(dir).resolve(asset.name()));

        modCache.add(result, mod.repository().toMod());
        return result;
    }

    private ResolvedMod resolve(ModRepository repository, @Nullable String modVersion, VersionArg version)
        throws ModDistributionException {
        if (!repository.supports(version)) {
            throw new ModDistributionException(repository.getName() + " does not support " + version);
        }

        GitHubRelease release = getRelease(repository, repository.getReleaseTag(version, modVersion).orElse(null));
        GitHubRelease.Asset asset = repository.findAsset(release, version)
            .orElseThrow(() -> new ModDistributionException(
                "Release %s of %s/%s contains no file for %s".formatted(
                    release.tagName(), repository.getOwner(), repository.getRepository(), version
                )
            ));

        return new ResolvedMod(repository, asset);
    }

    private GitHubRelease getRelease(ModRepository repository, @Nullable String tag) throws ModDistributionException {
        try {
            return tag == null
                ? api.getLatestRelease(repository.getOwner(), repository.getRepository())
                : api.getReleaseByTag(repository.getOwner(), repository.getRepository(), tag);
        } catch (ApiException | ProcessingException e) {
            throw new ModDistributionException(
                "Failed to get release %s of %s/%s".formatted(
                    tag == null ? "latest" : tag, repository.getOwner(), repository.getRepository()
                ), e
            );
        }
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
