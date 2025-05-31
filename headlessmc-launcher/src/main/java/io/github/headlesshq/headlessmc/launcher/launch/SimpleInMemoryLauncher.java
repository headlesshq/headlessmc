package io.github.headlesshq.headlessmc.launcher.launch;

import lombok.CustomLog;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A simple launcher for java programs that loads the classpath onto an {@link URLClassLoader}.
 */
@Setter
@CustomLog
public class SimpleInMemoryLauncher {
    private Function<URL[], ClassLoader> classLoaderFactory = urls -> new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

    public void simpleLaunch(URL[] classpathUrls, String mainClass, List<String> gameArgs) throws LaunchException {
        ClassLoader classLoaderBefore = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = null;
        try {
            classLoader = classLoaderFactory.apply(classpathUrls);
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> mainClassClass = Class.forName(mainClass, false, classLoader);
            Method main = mainClassClass.getDeclaredMethod("main", String[].class);
            main.setAccessible(true);
            main.invoke(null, (Object) gameArgs.toArray(new String[0]));
        } catch (InvocationTargetException e) {
            log.error(e);
            // not throwing????????????????????????
        } catch (Exception e) {
            throw new LaunchException("Failed to launch", e);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoaderBefore);
            if (classLoader instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) classLoader).close();
                } catch (Exception e) {
                    log.error("Failed to close ClassLoader.", e);
                }
            }
        }
    }

    public String getMainClassFromJar(File file) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                throw new IOException("Jar " + file + " did not contain a Manifest!");
            }

            String mainClass = manifest.getMainAttributes().getValue("Main-Class");
            if (mainClass == null) {
                throw new IOException("Jar " + file + " did not contain a Main-Class!");
            }

            return mainClass;
        }
    }

}
