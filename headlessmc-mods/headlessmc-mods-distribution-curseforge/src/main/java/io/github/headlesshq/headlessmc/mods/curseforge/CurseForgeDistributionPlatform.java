package io.github.headlesshq.headlessmc.mods.curseforge;

import io.github.headlesshq.headlessmc.config.Holder;
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
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * A {@link ModDistributionPlatform} for <a href=https://www.curseforge.com>CurseForge</a>.
 * The CurseForge API requires an api key, see {@link CurseForgeConfig#apiKey()}.
 * Mod ids are the slugs of CurseForge projects, but numeric project ids work as well.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CurseForgeDistributionPlatform implements ModDistributionPlatform {
    /**
     * The name of this platform.
     */
    public static final String NAME = "curseforge";

    private static final int PAGE_SIZE = 50;

    private final DownloadService downloadService;
    private final @RestClient CurseForgeAPI api;
    private final Holder<CurseForgeConfig> config;
    private final ModCache modCache;

    @Override
    public List<RemoteMod> search(String query, ModType type) throws HeadlessMcException {
        return search(query, null, null, type);
    }

    @Override
    public List<RemoteMod> search(String query, VersionArg version, ModType type) throws HeadlessMcException {
        return search(query, version.version(), modLoaderType(version, type), type);
    }

    private List<RemoteMod> search(
        String query,
        @Nullable String gameVersion,
        @Nullable Integer modLoaderType,
        ModType type
    ) throws HeadlessMcException {
        Integer classId = CurseForgeIds.classId(type);
        if (classId == null) {
            return List.of();
        }

        String apiKey = apiKey();
        try {
            return api.search(apiKey, CurseForgeIds.MINECRAFT, classId, query, null, null, gameVersion, modLoaderType, PAGE_SIZE)
                .data()
                .stream()
                .map(CurseForgeMod::toMod)
                .toList();
        } catch (ApiException | ProcessingException e) {
            throw new ModDistributionException("Failed to search CurseForge for " + query, e);
        }
    }

    @Override
    public Path download(
        Path dir,
        @Nullable Path worldDir,
        String id,
        VersionArg version,
        ModType type
    ) throws HeadlessMcException {
        if (ModType.MOD_PACK.equals(type)) {
            throw new ModDistributionException("Downloading modpacks from CurseForge is not supported yet");
        }

        String apiKey = apiKey();
        try {
            CurseForgeMod mod = findMod(apiKey, id, type);
            Integer modLoaderType = modLoaderType(version, type);
            CurseForgeFile file = api.getFiles(apiKey, mod.id(), version.version(), modLoaderType)
                .data()
                .stream()
                .filter(f -> f.supports(version.version()))
                .filter(f -> modLoaderType == null || f.supports(version.platform()))
                .max(Comparator.comparing(CurseForgeFile::fileDate))
                .orElseThrow(() -> new ModDistributionException(
                    "Failed to find any files for CurseForge project " + id + " for version " + version
                ));

            if (file.downloadUrl() == null) {
                throw new ModDistributionException((
                    "The author of %s does not allow third party downloads, "
                        + "please download %s manually from https://www.curseforge.com/minecraft/mc-mods/%s/files/%d"
                ).formatted(id, file.fileName(), mod.slug(), file.id()));
            }

            Path destination = type.directory().getDir(dir).resolve(file.fileName());
            if (ModType.DATA_PACK.equals(type)) {
                if (worldDir == null) {
                    throw new IllegalArgumentException("World not specified for datapack " + id);
                }

                destination = ModType.DATA_PACK.directory().getDir(worldDir).resolve(file.fileName());
            }

            try (DownloadContext context = downloadService.context()) {
                Path result = context.download(URI.create(file.downloadUrl()))
                    .hashes(file.hashesByAlgorithm())
                    .size(file.fileLength())
                    .progressBar(file.fileLength() > 10_000_000 ? "Downloading mod " + id : null)
                    .toFile(destination);

                modCache.add(result, mod.toMod());
                return result;
            }
        } catch (ApiException | ProcessingException e) {
            throw new ModDistributionException(
                "Failed to download " + id + " for version " + version + " to " + dir,
                e
            );
        }
    }

    private CurseForgeMod findMod(String apiKey, String id, ModType type) throws HeadlessMcException {
        if (id.chars().allMatch(Character::isDigit)) {
            return api.getMod(apiKey, Long.parseLong(id)).data();
        }

        Integer classId = CurseForgeIds.classId(type);
        if (classId == null) {
            throw new ModDistributionException("CurseForge does not support mods of type " + type.name());
        }

        return api.search(apiKey, CurseForgeIds.MINECRAFT, classId, null, id, null, null, null, null)
            .data()
            .stream()
            .filter(mod -> id.equalsIgnoreCase(mod.slug()))
            .findFirst()
            .orElseThrow(() -> new ModDistributionException("Failed to find CurseForge project " + id));
    }

    private static @Nullable Integer modLoaderType(VersionArg version, ModType type) {
        return ModType.MOD.equals(type) ? CurseForgeIds.modLoaderType(version.platform()) : null;
    }

    private String apiKey() throws ModDistributionException {
        return config.get()
            .apiKey()
            .filter(key -> !key.isBlank())
            .orElseThrow(() -> new ModDistributionException(
                "CurseForge requires an api key, please configure " + CurseForgeConfig.PREFIX + ".api-key"
            ));
    }

    @Override
    public boolean isEnabled() {
        return config.get().apiKey().filter(key -> !key.isBlank()).isPresent();
    }

    @Override
    public String getName() {
        return NAME;
    }

}
