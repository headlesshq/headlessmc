package io.github.headlesshq.headlessmc.wrapper.plugin;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Getter
public class TransformingClassloader extends URLClassLoader {
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("hmc.wrapper.debug.classes", "false"));

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final URLClassLoader transformerClassloader;
    private final List<TransformerPlugin> plugins;

    public TransformingClassloader(URL[] urls, ClassLoader parent, URLClassLoader transformerClassloader, List<TransformerPlugin> plugins) {
        super(urls, parent);
        this.transformerClassloader = transformerClassloader;
        this.plugins = plugins;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        try (InputStream is = this.getResourceAsStream(path)) {
            if (is == null) {
                throw new ClassNotFoundException(name);
            }

            byte[] classBytes = toByteArray(is);
            classBytes = instrument(name, classBytes);
            if (DEBUG) {
                Path savePath = Paths.get("build").resolve("transformation").resolve(path);
                Files.createDirectories(savePath.getParent().toAbsolutePath());
                try (OutputStream fos = Files.newOutputStream(savePath.toAbsolutePath())) {
                    fos.write(classBytes);
                }
            }

            return this.defineClass(name, classBytes, 0, classBytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            transformerClassloader.close();
        } finally {
            super.close();
        }
    }

    protected byte[] instrument(String name, byte[] classBytes) throws ClassNotFoundException {
        byte[] result = classBytes;
        for (TransformerPlugin transformer : plugins) {
            try {
                result = transformer.getTransformer().transform(name, result);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ClassNotFoundException(transformer.getName() + " failed to transform class " + name, e);
            }
        }

        return result;
    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        return baos.toByteArray();
    }

}
