package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.net.DownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ModrinthDistributionPlatform implements ModDistributionPlatform {
    private final ModpackDownloader modpackDownloader;
    private final DownloadService downloadService;
    private final @RestClient ModrinthAPI api;
    private final ModCache modCache;

    @Override
    public List<RemoteMod> search(String query, Set<ModType> types) throws HeadlessMcException {
        String facets = Facets.builder().modTypes(types).build().toQuery();
        try {
            ModrinthSearchResult result = api.search(query, facets);
            return result.hits().stream().map(ModrinthProject::toMod).toList();
        } catch (ApiException | ProcessingException e) {
            throw new ModDistributionException("Failed to search for " + query + ", " + facets, e);
        }
    }

    @Override
    public List<RemoteMod> search(String query, VersionArg version, Set<ModType> types) throws HeadlessMcException {
        List<String> loaders = List.of(version.platform());
        if (types.contains(ModType.DATA_PACK) || version.isVanilla()) {
            loaders = List.of();
        } else if (types.contains(ModType.SHADER)) {
            // TODO: this is just a quick and dirty fix,
            //  check what Shader platforms there actually are and which are supported
            loaders = "fabric".equalsIgnoreCase(version.platform())
                ? List.of("iris")
                : List.of("iris", "optifine");
        }

        String facets = Facets.builder()
            .loaders(loaders)
            .sides(version.side().stream().toList())
            .versions(List.of(version.version()))
            .modTypes(types)
            .build()
            .toQuery();

        try {
            ModrinthSearchResult result = api.search(query, facets);
            return result.hits().stream().map(ModrinthProject::toMod).toList();
        } catch (ApiException | ProcessingException e) {
            throw new ModDistributionException("Failed to search for " + query + ", " + facets, e);
        }
    }

    // TODO: this is kinda bad
    @Override
    public Path download(
        Path dir,
        @Nullable Path worldDir,
        String id,
        VersionArg version,
        ModType type
    ) throws HeadlessMcException {
        try {
            List<ModrinthProjectVersion> projectVersions = api.getProjectVersions(
                id, version.version(), null
            );

            // TODO: if Shader we actually need to check which loader "iris" or "optifine" etc.?
            Set<String> loaders = getLoaders(version, type);
            projectVersions = projectVersions.stream()
                .filter(pv -> pv.game_versions().contains(version.version()))
                .filter(pv -> pv.loaders().stream().anyMatch(loaders::contains))
                .toList();

            if (projectVersions.isEmpty()) {
                throw new ModDistributionException(
                    "Failed to find any versions for project " + id + " for version " + version
                );
            }

            ModrinthProjectVersion projectVersion = projectVersions.getFirst();
            ModrinthFile file = projectVersion.getPrimaryFile()
                .orElseThrow(() -> new ModDistributionException(
                    "Failed to get primary download file for project %s for version %s (%s)".formatted(
                        id,
                        version,
                        projectVersion
                    )
                ));

            ModrinthProject project = api.getProject(id);
            try (DownloadContext context = downloadService.context()) {
                DownloadBuilder downloadBuilder = context.download(URI.create(file.url()))
                    .hashes(file.hashes())
                    .size(file.size());

                if (ModType.MOD_PACK.equals(type)) {
                    modpackDownloader.download(
                        id, version.side().orElse(Side.CLIENT), dir, context, downloadBuilder
                    );

                    return dir;
                }

                Path destination = type.directory().getDir(dir).resolve(file.filename());
                if (ModType.DATA_PACK.equals(type)) {
                    if (worldDir == null) {
                        throw new IllegalArgumentException("World not specified for datapack " + id);
                    }

                    destination = ModType.DATA_PACK.directory().getDir(worldDir).resolve(file.filename());
                }

                Path result = downloadBuilder.progressBar(file.size() > 10_000_000 ? "Downloading mod " + id : null)
                    .toFile(destination);

                modCache.add(result, project.toMod());
                return result;
            }
        } catch (ApiException | ProcessingException e) {
            throw new ModDistributionException(
                "Failed to download " + id + " for version " + version + " to " + dir,
                e
            );
        }
    }

    private Set<String> getLoaders(VersionArg version, ModType type) {
        Set<String> loaders = new HashSet<>();
        if (ModType.RESOURCE_PACK.equals(type)) {
            // https://docs.modrinth.com/api/operations/getprojectversions/
            // In case of resource packs, use “minecraft”
            loaders.add("minecraft");
        } else if (ModType.DATA_PACK.equals(type)) {
            loaders.add(ModType.DATA_PACK.name());
        } else if (ModType.SHADER.equals(type)) {
            loaders.add("iris");
            if (!"fabric".equalsIgnoreCase(version.platform())) {
                loaders.add("optifine");
            }
        } else if (!version.isVanilla()) {
            loaders.add(version.platform());
        }

        return loaders;
    }

    @Override
    public String getName() {
        return ModDistributionPlatform.DEFAULT;
    }

}
