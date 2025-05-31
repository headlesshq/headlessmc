package io.github.headlesshq.headlessmc.wrapper;

import io.github.headlesshq.headlessmc.wrapper.plugin.TransformerPlugin;
import io.github.headlesshq.headlessmc.wrapper.plugin.TransformingClassloader;
import io.github.headlesshq.headlessmc.wrapper.plugin.TransformingPluginFinder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * The HeadlessMc wrapper will load the launcher through a custom Classloader.
 * There are two reasons for this:
 * <ol>
 *   <li>It allows us to implement a plugin system with transformer plugins.</li>
 *   <li>When launching the game in the same JVM, we can separate all game classes from the launcher classes.</li>
 * </ol>
 * The HeadlessMc wrapper packages the HeadlessMc launcher as a jar.
 * At runtime it will unpack that jar and create a {@link TransformingClassloader}
 * which has as classpath the HeadlessMc launcher jar and all jars found in the HeadlessMC/plugins directory.
 * It will also create a second classloader containing all jars found in the HeadlessMC/transformers directory.
 * The {@link TransformerPlugin}s in that Classloader will be found with a ServiceLoader
 * and are used to transform classes loaded on the {@link TransformingClassloader}.
 * This is a global API to access the HeadlessMc wrapper classes.
 */
public class HeadlessMcWrapper {
    /**
     * Allows you to define your own {@link TransformingPluginFinder} before {@link Main} is called.
     */
    @Setter
    @Getter
    private static Function<Path, TransformingPluginFinder> pluginFinderFactory = TransformingPluginFinder::new;

    /**
     * The {@link TransformingClassloader} used to load the headlessmc-launcher.
     */
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private static TransformingClassloader classLoader;

}
