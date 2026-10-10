package io.github.headlesshq.headlessmc.patcher;

import java.nio.file.Path;

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
     * {@link PatchContext#patch(Path, Patcher, PatchContext.PatchAction)}
     * and add system properties using {@link PatchContext#addSystemProperty(String, String)}.
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

    @Override
    default int compareTo(Patcher o) {
        return name().compareTo(o.name());
    }

    // eventually we might need multiple passes with patchers, etc.

}
