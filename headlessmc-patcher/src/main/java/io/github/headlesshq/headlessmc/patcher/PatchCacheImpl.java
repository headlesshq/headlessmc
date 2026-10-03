package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/**
 * Default implementation of {@link PatchCache}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PatchCacheImpl implements PatchCache {
    private final FileService fileService;
    private final HashService hashService;
    private final JsonService jsonService;
    private final AppFiles appFiles;
    private final McFiles mcFiles;

    @Override
    public Optional<Classpath> getCache(PatchContext context) {
        Path cacheDir = getCacheDir(context.getCacheKey());
        Path cacheInfo = cacheDir.resolve("cache.json");
        if (Files.exists(cacheInfo)) {
            CacheInfo info = jsonService.parse(cacheInfo, CacheInfo.class);
            try {
                SequencedSet<Path> classpath = resolve(context, cacheDir, info);
                return Optional.of(new Classpath(classpath));
            } catch (PatchException e) {
                try {
                    fileService.deleteFileAndEmptyParentDirs(cacheDir);
                } catch (FileException exception) {
                    e.addSuppressed(e);
                }

                log.error("Failed to resolve cache {}", cacheDir, e);
            }
        }

        return Optional.empty();
    }

    @Override
    public void saveCache(PatchContext context) {
        Path cacheDir = getCacheDir(context.getCacheKey());
        CacheInfo cacheInfo = createCacheInfo(cacheDir, context);
        jsonService.write(cacheDir.resolve("cache.json"), cacheInfo, true);
    }

    @Override
    public Key getCacheKey(Classpath classpath, List<Patcher> patchers, int javaVersion) {
        HashService.HashResult hashResult = hashService.hashFiles(classpath.files(), HashService.SHA256);
        Map<String, Long> patcherVersions = new HashMap<>();
        patchers.forEach(patcher -> patcherVersions.put(patcher.name(), patcher.version()));
        return new Key(hashResult.hash(), hashResult.size(), patcherVersions, javaVersion, getVersion());
    }

    @Override
    public Path getCacheDir(Key key) {
        return appFiles.getCacheDir()
            .resolve("patches")
            .resolve(String.valueOf(getVersion()))
            .resolve(String.valueOf(key.javaVersion()))
            .resolve(hashPatchers(key))
            .resolve(key.sha256());
    }

    @Override
    public long getVersion() {
        return 0L;
    }

    private String hashPatchers(Key key) {
        MessageDigest digest = hashService.getAlgorithm(HashService.SHA256);
        key.patchers().forEach((patcher, version) -> {
            digest.update(patcher.getBytes(StandardCharsets.UTF_8));
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(version).array());
        });

        return hashService.toHexString(digest.digest());
    }

    private SequencedSet<Path> resolve(PatchContext context, Path cacheDir, CacheInfo info) {
        SequencedSet<Path> classpath = new LinkedHashSet<>();
        for (Path library : context.getInitialClasspath().files()) {
            CacheInfo.PatchFile relative = relativize(library, cacheDir);
            if (!info.initialClasspath.contains(relative)) {
                throw new PatchException("Invalid cache for new classpath, could not find " + library);
            }
        }

        for (CacheInfo.PatchFile patchFile : info.initialClasspath) {
            Path library = patchFile.deRelativize(cacheDir, mcFiles.getMcDir(), mcFiles.getLibraryDir());
            if (!context.getInitialClasspath().files().contains(library)) {
                throw new PatchException(
                    "Invalid cache for new classpath, cache lists " + library + " but it was not in initial classpath"
                );
            }
        }

        for (CacheInfo.PatchFile patchFile : info.currentClasspath) {
            Path library = patchFile.deRelativize(cacheDir, mcFiles.getMcDir(), mcFiles.getLibraryDir());
            if (!Files.exists(library)) {
                throw new PatchException("Failed to find patch file " + library + " (" + patchFile + ")");
            }

            classpath.add(library);
        }

        return classpath;
    }

    private CacheInfo createCacheInfo(Path cacheDir, PatchContext context) {
        SequencedSet<CacheInfo.PatchFile> initialClasspath = new LinkedHashSet<>();
        SequencedSet<CacheInfo.PatchFile> currentClasspath = new LinkedHashSet<>();
        for (Path initial : context.getInitialClasspath().files()) {
            initialClasspath.add(relativize(initial, cacheDir));
        }

        for (Path current : context.getCurrentClasspath().files()) {
            currentClasspath.add(relativize(current, cacheDir));
        }

        return new CacheInfo(
            initialClasspath,
            currentClasspath
        );
    }

    private CacheInfo.PatchFile relativize(Path file, Path cacheDir) {
        if (file.startsWith(cacheDir)) {
            return new CacheInfo.PatchFile(cacheDir.relativize(file).toString(), CacheInfo.RelativeTo.CACHE);
        } else if (file.startsWith(mcFiles.getLibraryDir())) {
            return new CacheInfo.PatchFile(
                mcFiles.getLibraryDir().relativize(file).toString(),
                CacheInfo.RelativeTo.MC_LIBRARIES
            );
        } else if (file.startsWith(mcFiles.getMcDir())) {
            return new CacheInfo.PatchFile(mcFiles.getMcDir().relativize(file).toString(), CacheInfo.RelativeTo.MC);
        }

        return new CacheInfo.PatchFile(file.toString(), CacheInfo.RelativeTo.NONE);
    }

    @RegisterForReflection
    private record CacheInfo(
        SequencedSet<PatchFile> initialClasspath,
        SequencedSet<PatchFile> currentClasspath
    ) implements ReflectionRegistered {
        @RegisterForReflection
        record PatchFile(String path, RelativeTo relativeTo) implements ReflectionRegistered {
            Path deRelativize(Path cacheDir, Path mcDir, Path mcLibraryDir) {
                return switch (relativeTo) {
                    case NONE -> cacheDir.getFileSystem().getPath(path);
                    case CACHE -> cacheDir.resolve(path);
                    case MC -> mcDir.resolve(path);
                    case MC_LIBRARIES -> mcLibraryDir.resolve(path);
                };
            }
        }

        @RegisterForReflection
        enum RelativeTo implements ReflectionRegistered {
            NONE,
            CACHE,
            MC,
            MC_LIBRARIES
        }
    }

}
