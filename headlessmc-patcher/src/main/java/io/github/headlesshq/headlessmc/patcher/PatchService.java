package io.github.headlesshq.headlessmc.patcher;

import java.util.List;
import java.util.Optional;

/**
 * Allows you to patch libraries with {@link Patcher}s.
 * May perform caching with {@link PatchCache}.
 */
public interface PatchService {
    Classpath patch(Classpath classpath, int javaVersion, List<Patcher> patchers);

    List<Patcher> getPatchers();

    Optional<Patcher> getPatcher(String name);

    default Patcher requirePatcher(String name) {
        return getPatcher(name).orElseThrow(() -> new PatchException("Failed to find patcher '" + name + "'"));
    }

}
