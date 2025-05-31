package io.github.headlesshq.headlessmc.modlauncher;

import dev.xdark.deencapsulation.Deencapsulation;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link URLClassLoader} that looks through the modules of the given java.lang.module.Configuration to find classes.
 */
@SuppressWarnings({"Since15", "RedundantSuppression"})
public class ModuleURLClassLoader extends URLClassLoader implements Java9Classloader {
    private static final Logger LOGGER = Logger.getLogger(ModuleURLClassLoader.class.getName());
    private static final MethodHandle LAYER_BIND_TO_LOADER;

    private final AtomicInteger classesLoaded = new AtomicInteger();
    private Configuration configuration;

    static {
        ClassLoader.registerAsParallelCapable();

        try {
            Deencapsulation.deencapsulate(MethodHandles.Lookup.class);
            Field hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            hackfield.setAccessible(true);

            MethodHandles.Lookup hack = (MethodHandles.Lookup) hackfield.get(null);
            LAYER_BIND_TO_LOADER = hack.findSpecial(ModuleLayer.class, "bindToLoader", MethodType.methodType(Void.TYPE, ClassLoader.class), ModuleLayer.class);
        } catch (IllegalAccessException | NoSuchMethodException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Constructs a new ModuleURLClassLoader for the given classpath and parent loader.
     * For this classloader to work properly .setConfiguration needs to be called!
     *
     * @param urls the classpath this Classloader loads from.
     * @param parent the parent Classloader to delegate to.
     */
    public ModuleURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> findClass(String moduleName, String name) {
        try {
            LOGGER.fine("Classes loaded: " + classesLoaded.incrementAndGet());
            Optional<ResolvedModule> module = configuration.findModule(moduleName);
            if (!module.isPresent()) {
                LOGGER.severe("Failed to find module " + moduleName + " for class " + name + ", using default findClass");
                return findClass(name);
            }

            LOGGER.fine("Found module " + module.get() + " for class " + name);
            Class<?> clazz = loadFromModule(module.get(), name);
            if (clazz == null) {
                LOGGER.severe("Failed to find class " + name + " in module " + module + ", using default findClass");
                return findClass(name);
            }

            return clazz;
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Failed to find class " + name + " in module " + moduleName + ", returning null...");
            return null;
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        LOGGER.fine("Classes loaded: " + classesLoaded.incrementAndGet());
        LOGGER.fine("Searching for " + name);
        // this is like the least performant thing ever,
        // but the ModuleURLClassLoader will only load about 183 classes.
        for (ResolvedModule module : configuration.modules()) {
            LOGGER.fine("Checking " + module.name() + " for " + name);
            Class<?> clazz = loadFromModule(module, name);
            if (clazz != null) {
                return clazz;
            }
        }

        LOGGER.fine("Failed to find: " + name + " in modules, using findClass");
        return super.findClass(name);
    }

    /**
     * Sets the Configuration that this Classloader looks up classes and modules in.
     *
     * @param configuration the configuration this Classloader will lookup from.
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    private @Nullable Class<?> loadFromModule(ResolvedModule module, String name) {
        ModuleReference ref = module.reference();
        try (ModuleReader reader = ref.open()) {
            String rn = name.replace('.', '/').concat(".class");
            ByteBuffer bb = reader.read(rn).orElse(null);
            if (bb == null) {
                return null;
            }

            try {
                LOGGER.fine("Found class " + name + " in module " + module.name());
                return defineClass(name, bb, getCodeSource(ref));
            } finally {
                reader.release(bb);
            }
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Failed to find " + name + " in module " + module, e);
        }

        return null;
    }

    // jdk.internal.loader.Loader.LoadedModule
    private CodeSource getCodeSource(ModuleReference reference) {
        URL url = null;
        if (reference.location().isPresent()) {
            try {
                url = reference.location().get().toURL();
            } catch (MalformedURLException | IllegalArgumentException ignored) { }
        }

        return new CodeSource(url, (CodeSigner[]) null);
    }

    /**
     * Binds the given classloader to the given layer.
     * Calls the package-private method ModuleLayer.bindToLoader for the given classloader on the given layer.
     *
     * @param classLoader the classloader to bind to the layer.
     * @param layer the layer to bind the classloader to.
     */
    public static void bindToLayer(ModuleURLClassLoader classLoader, ModuleLayer layer) {
        try {
            LAYER_BIND_TO_LOADER.invokeExact((ModuleLayer) layer, (ClassLoader) classLoader);
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

}