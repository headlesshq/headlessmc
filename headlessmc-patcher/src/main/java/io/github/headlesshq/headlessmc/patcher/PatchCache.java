package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Caches the result of a run of {@link PatchService}.
 * We store a hash of the original classpath,
 * and the names and versions of the patchers used
 * as the cache {@link Key}.
 * That should be enough to detect if the classpath
 * needs re-patching.
 */
public interface PatchCache {
    Optional<PatchResult> getCache(PatchContext context);

    void saveCache(PatchContext context);

    Key getCacheKey(PatchResult patchResult, List<Patcher> patchers, int javaVersion);

    Path getCacheDir(Key key);

    long getVersion();

    /**
     * Identifies a patched, cached classpath.
     *
     * @param sha256      the sha256 hash of all libraries on the initial,
     *                    unpatched classpath.
     * @param size        the total size of all libraries on the initial,
     *                    unpatched classpath.
     * @param patchers    the map of {@link Patcher} names to their versions
     *                    that were used to patch the initial classpath.
     * @param javaVersion the version of Java that was used to perform the
     *                    patching.
     * @param version     the version of the cache and general
     *                    patching mechanism used.
     */
    @RegisterForReflection
    record Key(String sha256, long size, Map<String, Long> patchers, int javaVersion, long version)
        implements ReflectionRegistered {}

}
