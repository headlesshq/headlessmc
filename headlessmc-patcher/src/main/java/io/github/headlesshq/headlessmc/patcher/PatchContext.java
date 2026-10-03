package io.github.headlesshq.headlessmc.patcher;

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
     * @return the initial classpath the patch run has been started with.
     */
    Classpath getInitialClasspath();

    /**
     * @return the current state of the classpath,
     * potentially with new additions or modified files.
     */
    Classpath getCurrentClasspath();

    /**
     * Allows {@link Patcher}s to add files to the classpath.
     * The file will be present in the {@link #getCurrentClasspath()},
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
     * The file will be present in the {@link #getCurrentClasspath()},
     * once the returned {@link OutputStream} has been closed.
     *
     * @see Classpath#javaAgents()
     * @param library the name of the javaagent to add
     *                (a ".jar" will be appended for the file name)
     * @param patcher the patcher adding the library.
     * @return an {@link OutputStream} for the file that will be added as
     * JavaAgent
     */
    OutputStream addAgent(String library, Patcher patcher) throws IOException;

    void patch(Path library, Patcher patcher, PatchAction action);

    List<Patcher> getPatchers();

    int getJavaVersion();

    <C extends HelperService> Stream<C> services(Class<C> type);

    PatchCache.Key getCacheKey();

    @FunctionalInterface
    interface PatchAction {
        boolean apply(JarFile source, JarOutputStream destination) throws IOException;
    }

}
