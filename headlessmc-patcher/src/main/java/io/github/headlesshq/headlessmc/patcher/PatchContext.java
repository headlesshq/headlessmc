package io.github.headlesshq.headlessmc.patcher;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * This interface provides {@link Patcher}s with the means
 * to change the classpath.
 */
public interface PatchContext {
    /**
     * @return the initial classpath, agents and system properties the patch run has been started with.
     */
    PatchResult getInitialPatchResult();

    /**
     * @return the current state of the classpath, agents and system properties,
     * potentially with new additions or modified files.
     */
    PatchResult getCurrentPatchResult();

    /**
     * Allows {@link Patcher}s to add files to the classpath.
     * The file will be present in the {@link #getCurrentPatchResult()},
     * once the returned {@link OutputStream} has been closed.
     *
     * @param library the name of the library to add
     *                (a ".jar" will be appended for the file name)
     * @param patcher the patcher adding the library.
     * @return an {@link OutputStream} for the file that will be added to
     * the classpath.
     */
    OutputStream add(String library, Patcher patcher) throws IOException;

    /**
     * Allows {@link Patcher}s to add JavaAgents to the classpath.
     * The file will be present in the {@link #getCurrentPatchResult()},
     * once the returned {@link OutputStream} has been closed.
     *
     * @see PatchResult#javaAgents()
     * @param library the name of the javaagent to add
     *                (a ".jar" will be appended for the file name)
     * @param patcher the patcher adding the library.
     * @return an {@link OutputStream} for the file that will be added as
     * JavaAgent
     */
    OutputStream addAgent(String library, Patcher patcher) throws IOException;

    void patch(Path library, Patcher patcher, PatchAction action);

    /**
     * Allows {@link Patcher}s to add system properties, which the patched classpath needs to work.
     * E.g. the LWJGL patcher needs JOML to not use {@code sun.misc.Unsafe}.
     * The system property will be present in the {@link #getCurrentPatchResult()}
     * and is cached together with the classpath.
     *
     * @param key   the name of the system property.
     * @param value the value of the system property.
     */
    void addSystemProperty(String key, @Nullable String value);

    List<Patcher> getPatchers();

    int getJavaVersion();

    <C extends HelperService> Stream<C> services(Class<C> type);

    PatchCache.Key getCacheKey();

    @FunctionalInterface
    interface PatchAction {
        boolean apply(JarFile source, JarOutputStream destination) throws IOException;
    }

}
