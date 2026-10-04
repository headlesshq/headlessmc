package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads classes from jmod files, which were introduced with Java 9.
 */
@Slf4j
@RequiredArgsConstructor
public class JModClassSource implements ClassSource {
    private final ZipFile jmodFile;

    @Override
    public Optional<ClassReader> getClass(String typeName) {
        ZipEntry entry = jmodFile.getEntry("classes/" + typeName + ".class");
        if (entry != null) {
            try (InputStream inputStream = jmodFile.getInputStream(entry)) {
                return Optional.of(new ClassReader(inputStream));
            } catch (IOException e) {
                log.error("Failed to open {} in {}", entry.getName(), jmodFile.getName());
            }
        }

        return Optional.empty();
    }

    @Override
    public void close() {
        try {
            jmodFile.close();
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to close file " + jmodFile, e);
        }
    }

}
