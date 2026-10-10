package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.exceptions.ExceptionUtil;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.quarkus.runtime.ImageMode;
import jakarta.enterprise.context.Dependent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@Dependent
public class ClassLoaderSuperClassStrategy implements SuperClassStrategy {
    @Override
    public boolean isApplicable() {
        return !ImageMode.current().isNativeImage();
    }

    @Override
    public int sort() {
        return SORT_CLASS_LOADER;
    }

    @Override
    public SuperClassResolver apply(PatchContext context) {
        URL[] urls = context.getCurrentPatchResult().files().stream()
            .map(Path::toAbsolutePath)
            .map(Path::toUri)
            .map(this::toURL)
            .toArray(URL[]::new);

        //noinspection resource
        URLClassLoader classLoader = new URLClassLoader(urls);
        return new SuperClassResolver() {
            @Override
            public String getCommonSuperClass(String type1, String type2) {
                try {
                    Class<?> class1;
                    try {
                        class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
                    } catch (ClassNotFoundException e) {
                        throw new PatchException(type1, e);
                    }
                    Class<?> class2;
                    try {
                        class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
                    } catch (ClassNotFoundException e) {
                        throw new PatchException(type2, e);
                    }
                    if (class1.isAssignableFrom(class2)) {
                        return type1;
                    }
                    if (class2.isAssignableFrom(class1)) {
                        return type2;
                    }
                    if (class1.isInterface() || class2.isInterface()) {
                        return "java/lang/Object";
                    } else {
                        do {
                            class1 = class1.getSuperclass();
                        } while (!class1.isAssignableFrom(class2));
                        return class1.getName().replace('.', '/');
                    }
                } catch (/*okay-to-catch-marker*/Throwable throwable) {
                    throw new PatchException(ExceptionUtil.handleInterruptions(throwable));
                }
            }

            @Override
            public void close() throws HeadlessMcIOException {
                try {
                    classLoader.close();
                } catch (IOException e) {
                    throw new HeadlessMcIOException(e);
                }
            }
        };
    }

    private URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new PatchException("Failed to create URL from URI: " + uri);
        }
    }

}
