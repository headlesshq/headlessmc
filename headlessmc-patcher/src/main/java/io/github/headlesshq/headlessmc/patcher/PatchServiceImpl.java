package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link PatchService}.
 */
@ApplicationScoped
public class PatchServiceImpl implements PatchService {
    private final Instance<HelperService> services;
    private final Instance<Patcher> patchers;
    private final FileService fileService;
    private final PatchCache cache;
    private final McFiles mcFiles;

    @Inject
    public PatchServiceImpl(
        @Any Instance<HelperService> services,
        @Any Instance<Patcher> patchers,
        FileService fileService,
        PatchCache cache,
        McFiles mcFiles
    ) {
        this.services = services;
        this.patchers = patchers;
        this.fileService = fileService;
        this.mcFiles = mcFiles;
        this.cache = cache;
    }

    @Override
    public Classpath patch(Classpath classpath, int javaVersion, List<Patcher> patchers) {
        if (patchers.isEmpty()) {
            return classpath;
        }

        PatchCache.Key key = cache.getCacheKey(classpath, patchers, javaVersion);
        Path cacheDir = cache.getCacheDir(key);
        PatchContext context = new PatchContextImpl(
            services::stream, classpath, fileService, key, patchers, mcFiles, javaVersion, cacheDir, classpath
        );

        Optional<Classpath> cached = cache.getCache(context);
        if (cached.isPresent()) {
            return cached.get();
        }

        for (Patcher patcher : patchers) {
            patcher.patch(context);
        }

        cache.saveCache(context);
        return context.getCurrentClasspath();
    }

    @Override
    public List<Patcher> getPatchers() {
        return patchers.stream().sorted().toList();
    }

    @Override
    public Optional<Patcher> getPatcher(String name) {
        return patchers.stream().filter(patcher -> patcher.name().equalsIgnoreCase(name)).findFirst();
    }

}
