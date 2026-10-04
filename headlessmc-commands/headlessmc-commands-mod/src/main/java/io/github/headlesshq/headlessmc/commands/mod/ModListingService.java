package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.ModTypeService;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ModListingService {
    private final PlatformService platformService;
    private final ProfileResolver profileResolver;
    private final ModTypeService modTypeService;
    private final ModCache modCache;

    public List<ModFile> list(
        List<String> versionArg,
        @Nullable String platformName,
        @Nullable String world,
        @Nullable String type
    ) {
        if (versionArg.isEmpty()) {
            throw new IllegalArgumentException("Please specify the profile to list the mods of.");
        }

        Profile profile = profileResolver.resolve(versionArg, Side.BOTH);
        return list(profile, platformName, world, type);
    }

    public List<ModFile> list(
        Profile profile,
        @Nullable String platformName,
        @Nullable String world,
        @Nullable String type
    ) {
        Platform platform = platformService.getPlatform(platformName == null ? profile.version().platform() : platformName)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to resolve platform " + profile.version().platform() + " for profile " + profile.name()
            ));

        if (type == null) {
            Set<ModType> types = new HashSet<>(modTypeService.getAllModTypes());
            types.remove(ModType.PLUGIN);
            types.remove(ModType.MOD);
            platform.getModSupport().map(ModSupport::modTypes).ifPresent(types::addAll);
            if (world != null) {
                types = Set.of(ModType.DATA_PACK);
            }

            List<ModFile> result = new ArrayList<>();
            for (ModType modType : types) {
                result.addAll(collectModsOfType(profile, world, platform, modType));
            }

            return result;
        }

        ModType modType = modTypeService.getByName(type)
            .orElseThrow(() -> new IllegalArgumentException(
                "Failed to find mod type " + type + ", available: " + modTypeService.getAllModTypes()
            ));

        return collectModsOfType(profile, world, platform, modType);
    }

    private List<ModFile> collectModsOfType(Profile profile, @Nullable String world, Platform platform, ModType type) {
        if (ModType.MOD_PACK.equals(type)) {
            // TODO: ModCache save downloaded ModPacks!
            return List.of();
        }

        if (ModType.DATA_PACK.equals(type)) {
            List<ModFile> result = new ArrayList<>();
            if (world != null) {
                Path worldDir = Worlds.resolveWorld(profile, world);
                result.addAll(collectModsOfType(platform, type, type.directory().getDir(worldDir), world));
            } else {
                for (Path worldDir : Worlds.listWorlds(profile)) {
                    result.addAll(collectModsOfType(platform, type, type.directory().getDir(worldDir), worldDir.getFileName().toString()));
                }
            }

            return result;
        }

        Path dir = type.directory().getDir(profile.path());
        return collectModsOfType(platform, type, dir, null);
    }

    private List<ModFile> collectModsOfType(Platform platform, ModType type, Path dir, @Nullable String world) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> fileList = Files.list(dir)) {
            List<Path> modFiles = fileList.filter(Files::isRegularFile).toList();
            List<ModFile> result = new ArrayList<>();
            for (Path modFile : modFiles) {
                result.addAll(read(modFile, platform, type, world));
            }

            return result;
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

    private List<ModFile> read(Path file, Platform platform, ModType type, @Nullable String world) {
        List<ModFile> result = new ArrayList<>();
        ModSupport modSupport = platform.getModSupport().orElse(null);
        if (modSupport != null && modSupport.modTypes().contains(type)) {
            for (ModReader reader : modSupport.modReaders()) {
                try {
                    reader.read(file).stream()
                        .map(mod -> new ModFile(type, file, mod.id(), mod.name(), mod.description(), mod.authors(), Optional.ofNullable(world)))
                        .forEach(result::add);
                } catch (HeadlessMcException e) {
                    log.info("Failed to read file {} as {} {}", file, platform.getName(), type.name(), e);
                }
            }
        } else {
            result.add(modCache.find(file)
                .map(mod -> new ModFile(type, file, mod.id(), mod.name(), Optional.ofNullable(mod.description()), List.of(), Optional.ofNullable(world)))
                .orElse(new ModFile(type, file, file.getFileName().toString(), file.getFileName().toString(), Optional.of("?"), List.of(), Optional.ofNullable(world))));
        }

        return result;
    }

}
