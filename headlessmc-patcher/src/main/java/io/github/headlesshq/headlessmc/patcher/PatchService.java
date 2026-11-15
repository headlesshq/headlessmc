package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.McFiles;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /**
     * Finds the patchers to apply for a selection of patcher names and solves the conflicts between them:
     * a patcher that is {@link Patcher#excludes() excluded} by another selected patcher is left out.
     * This is how a selection has to be turned into patchers. The names can come from several places
     * that do not know about each other, like a profile and the profile it inherits from,
     * so conflicts can only be solved once all of them are known.
     *
     * @param names the names of the selected patchers, case is ignored, as are duplicates.
     * @return the patchers to apply, in the order they have been selected in.
     * @throws PatchException if a patcher cannot be found, or if two selected patchers exclude each other.
     */
    default List<Patcher> resolve(Collection<String> names) throws PatchException {
        Map<String, Patcher> selected = new LinkedHashMap<>();
        for (String name : names) {
            Patcher patcher = requirePatcher(name);
            selected.putIfAbsent(patcher.name().toLowerCase(Locale.ROOT), patcher);
        }

        Map<String, Patcher> excluded = new LinkedHashMap<>();
        for (Patcher patcher : selected.values()) {
            for (String name : patcher.excludes()) {
                Patcher other = selected.get(name.toLowerCase(Locale.ROOT));
                if (other == null || other == patcher) {
                    continue;
                }

                if (other.excludes().stream().anyMatch(patcher.name()::equalsIgnoreCase)) {
                    throw new PatchException(
                        "Patchers '" + patcher.name() + "' and '" + other.name()
                            + "' exclude each other, select only one of them"
                    );
                }

                excluded.put(other.name().toLowerCase(Locale.ROOT), patcher);
            }
        }

        return selected.entrySet().stream()
            .filter(entry -> !excluded.containsKey(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();
    }

}
