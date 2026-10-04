package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.PatchContext;
import org.objectweb.asm.ClassWriter;

/**
 * When rewriting classes, if we want our {@link ClassWriter}
 * to compute stack frames ({@link ClassWriter#COMPUTE_FRAMES}),
 * we need to be able to find out the common superclass of two given
 * java classes ({@code ClassWriter.getCommonSuperClass(String, String)}).
 * The easiest way would be to use the default implementation
 * of {@link ClassWriter}, which attempts to load the two classes,
 * using its ClassLoader and then matching the super classes
 * of the two classes. However,
 * this can be a bad idea for a multitude of reasons:
 * <ol>
 *     <li>The classes are not on the classpath
 *     (we may solve this by loading the jar containing them
 *     with another ClassLoader).</li>
 *     <li>The classes were written for a newer version of Java,
 *     and we cannot load them in this JVM. This can definitely happen
 *     if HeadlessMc was launched with an older version of Java
 *     then the Mc version we want to launch requires.
 *     This is also a big problem if the classes inherit from JDK classes,
 *     as the type hierarchy there can also change between Java versions.
 *     </li>
 *     <li>We are in a native Image and cannot load classes dynamically.</li>
 * </ol>
 * To solve this we introduce this interface.
 * Implementations represent different ways to get the common superclass
 * for two classes via {@link SuperClassResolver}s.
 */
public interface SuperClassStrategy extends Comparable<SuperClassStrategy> {
    int SORT_NON_CLASS_LOADING = 100;
    int SORT_PROBE = 200;
    // TODO: if currentJava == PatchContext.java && not in native image, use classloader first
    int SORT_CLASS_LOADER = 300;

    /**
     * Creates a new {@link SuperClassResolver}
     * for the given context and classpath.
     * The returned object must be closed ({@link AutoCloseable}),
     * after use.
     *
     * @param context the context in which we are patching.
     * @return a resolver for resolving the common super class of two classes.
     * @see SuperClassResolver#getCommonSuperClass(String, String)
     */
    SuperClassResolver apply(PatchContext context);

    /**
     * Some strategies (loading the classes into the current JVM) may
     * not be applicable, e.g. if we are in a native Image.
     *
     * @return {@code true} if this strategy can be applied in the current context.
     */
    boolean isApplicable();

    /**
     * @return an ordinal to sort CommonSuperClassStrategies.
     */
    int sort();

    @Override
    default int compareTo(SuperClassStrategy o) {
        return Integer.compare(sort(), o.sort());
    }

}
