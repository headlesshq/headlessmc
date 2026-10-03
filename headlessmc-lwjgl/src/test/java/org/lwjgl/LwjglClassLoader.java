package org.lwjgl;

import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.lwjgl.api.Transformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * This classloader can load all classes inside the {@code
 * io.github.headlesshq.headlessmc.lwjgl.lwjgltestclasses} package (which are on the
 * SystemClassLoaders classpath), running them through the given {@link
 * Transformer}. Every other class will be loaded by the SystemClassLoader.
 */
@RequiredArgsConstructor
public class LwjglClassLoader extends ClassLoader {
    private final Transformer transformer;

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
        Class<?> clazz = findClass(name);
        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        if (path.startsWith("org/lwjgl")) {
            try (InputStream is = getParent().getResourceAsStream(path)) {
                if (is == null) {
                    throw new ClassNotFoundException("Couldn't load " + path);
                }

                ClassReader cr = new ClassReader(is);
                ClassNode cn = new ClassNode();
                cr.accept(cn, 0);

                transformer.transform(cn);

                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                    @Override
                    protected ClassLoader getClassLoader() {
                        return LwjglClassLoader.this;
                    }
                };

                cn.accept(cw);
                byte[] clazzBytes = cw.toByteArray();
                return this.defineClass(name, clazzBytes, 0, clazzBytes.length);
            } catch (IOException ioe) {
                throw new ClassNotFoundException(ioe.getMessage());
            }
        }

        return getParent().loadClass(name);
    }

}
