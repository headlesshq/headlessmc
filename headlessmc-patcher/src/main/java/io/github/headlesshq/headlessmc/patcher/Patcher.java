package io.github.headlesshq.headlessmc.patcher;

import java.nio.file.Path;
import java.util.Map;

/**
 * Represents a Patcher that patches a certain library.
 * A patcher can e.g. patch Log4J, to fix the Log4Shell exploit,
 * or for HeadlessMc, turn the LWJGL library into stubs,
 * so the Minecraft client runs headlessly, without rendering.
 */
public interface Patcher extends Comparable<Patcher> {
    /**
     * Runs this patcher on the given {@link PatchContext}.
     * A patcher can modify the classpath using
     * {@link PatchContext#patch(Path, Patcher, PatchContext.PatchAction)}.
     *
     * @param context the context to use for patching.
     */
    void patch(PatchContext context);

    /**
     * @return an identifier for this patcher.
     */
    String name();

    /**
     * We try to cache a patched classpath.
     * To do that, we store the hash of the original classpath
     * and the names and versions of every involved patcher.
     * If we update HeadlessMc and Patchers have changed,
     * they are to return a new version, invalidating
     * the stored information and causing a re-patch of
     * the classpath.
     *
     * @return the version of this patcher.
     */
    long version();

    /**
     * Some patches only work if the game is launched with certain system properties,
     * e.g. the LWJGL patcher needs JOML to not use {@code sun.misc.Unsafe}.
     * These are added to the game, unless they have already been specified.
     *
     * @return the system properties required by this patcher.
     */
    default Map<String, String> systemProperties() {
        return Map.of();
    }

    @Override
    default int compareTo(Patcher o) {
        return name().compareTo(o.name());
    }

    // eventually we might need multiple passes with patchers, etc.

}
