package io.github.headlesshq.headlessmc.plugins.augmenter;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the native image sources of a {@code quarkus-app} mutable jar with plugins in its providers directory:
 * <pre>{@code
 * java -cp quarkus-app/quarkus-run.jar:headlessmc-plugins-augmenter.jar \
 *     io.github.headlesshq.headlessmc.plugins.augmenter.NativeAugmenterMain <quarkus-app>
 * }</pre>
 * Quarkus can re-augment a mutable jar itself ({@code -Dquarkus.launch.rebuild=true}), but always into a jar,
 * its {@code ReaugmentTask} forces {@code quarkus.native.enabled=false}.
 * Like the {@code QuarkusEntryPoint} does for the {@code ReaugmentTask}, this class loads the deployment
 * class path of the mutable jar into a class loader and runs the {@link NativeAugmentTask} in it.
 * The result is placed into {@code <quarkus-app>/../native-sources}.
 */
public final class NativeAugmenterMain {
    private NativeAugmenterMain() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: NativeAugmenterMain <quarkus-app directory of a mutable jar>");
            System.exit(1);
        }

        Path appRoot = Path.of(args[0]).toAbsolutePath().normalize();
        Path classPathFile = appRoot.resolve("lib").resolve("deployment").resolve("deployment-class-path.dat");
        if (!Files.isRegularFile(classPathFile)) {
            System.err.println(appRoot + " is not the quarkus-app directory of a mutable jar, " + classPathFile + " is missing");
            System.exit(1);
        }

        List<URL> urls = new ArrayList<>();
        for (String path : readClassPath(classPathFile)) {
            urls.add(appRoot.resolve(path).toUri().toURL());
        }

        urls.add(getOwnJar().toUri().toURL());
        try (URLClassLoader loader = new ChildFirstClassLoader(urls.toArray(new URL[0]), NativeAugmenterMain.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass(NativeAugmentTask.class.getName())
                .getDeclaredMethod("main", Path.class)
                .invoke(null, appRoot);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readClassPath(Path file) throws IOException, ClassNotFoundException {
        try (InputStream inputStream = Files.newInputStream(file); ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (List<String>) objectInputStream.readObject();
        }
    }

    private static Path getOwnJar() throws URISyntaxException {
        return Path.of(NativeAugmenterMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    /**
     * Loads the classes of this module itself, so that the {@link NativeAugmentTask}
     * sees the deployment class path instead of being loaded by the system class loader.
     */
    private static final class ChildFirstClassLoader extends URLClassLoader {
        private static final String OWN_PACKAGE = NativeAugmenterMain.class.getPackageName() + ".";

        ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(OWN_PACKAGE)) {
                return super.loadClass(name, resolve);
            }

            synchronized (getClassLoadingLock(name)) {
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    type = findClass(name);
                }

                if (resolve) {
                    resolveClass(type);
                }

                return type;
            }
        }
    }

}
