package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.McFiles;
import java.util.List;
import java.util.Optional;

/**
 * Allows you to patch libraries with {@link Patcher}s.
 * May perform caching with {@link PatchCache}.
 */
public interface PatchService {
    /**
     * Applies the given patchers to a classpath.
     *
     * @param mcFiles     the {@code .minecraft} directory the classpath belongs to,
     *                    used to recognize its libraries in the patch cache.
     * @param patchResult the unpatched classpath.
     * @param javaVersion the Java version the classpath is going to be launched with.
     * @param patchers    the patchers to apply.
     * @return the patched classpath.
     */
    PatchResult patch(McFiles mcFiles, PatchResult patchResult, int javaVersion, List<Patcher> patchers);

    List<Patcher> getPatchers();

    Optional<Patcher> getPatcher(String name);

    default Patcher requirePatcher(String name) {
        return getPatcher(name).orElseThrow(() -> new PatchException("Failed to find patcher '" + name + "'"));
    }

}
