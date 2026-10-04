package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import org.objectweb.asm.ClassReader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * Represents a way to get the bytecode of a class file
 * as a {@link ClassReader}.
 */
public interface ClassSource extends Closeable {
    Optional<ClassReader> getClass(String typeName);
    
    @FunctionalInterface
    interface Provider {
        ClassSource open() throws HeadlessMcIOException;
        
        static Provider zip(Path zipFile, Function<ZipFile, ClassSource> factory) {
            return () -> {
                try {
                    ZipFile zip = new ZipFile(zipFile.toFile());
                    return factory.apply(zip);
                } catch (IOException e) {
                    throw new HeadlessMcIOException("Failed to open " + zipFile, e);
                }
            };
        }

        static Provider jar(Path jarFile, int javaVersion, Function<JarFile, ClassSource> factory) {
            return () -> {
                try {
                    JarFile zip = new JarFile(
                        jarFile.toFile(),
                        true,
                        ZipFile.OPEN_READ,
                        Runtime.Version.parse(String.valueOf(javaVersion))
                    );

                    return factory.apply(zip);
                } catch (IOException e) {
                    throw new HeadlessMcIOException("Failed to open " + jarFile, e);
                }
            };
        }
    }

}
