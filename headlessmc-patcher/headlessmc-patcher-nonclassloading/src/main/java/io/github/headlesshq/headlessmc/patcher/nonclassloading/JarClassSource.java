package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.exceptions.ExceptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Slf4j
@RequiredArgsConstructor
public class JarClassSource implements ClassSource {
    private final JarFile file;

    @Override
    public Optional<ClassReader> getClass(String typeName) {
        ZipEntry entry = file.getEntry(typeName + ".class");
        if (entry == null) {
            return Optional.empty();
        }

        try (InputStream inputStream = file.getInputStream(entry)) {
            return Optional.of(new ClassReader(inputStream));
        } catch (/* okay to catch marker */Exception e) {
            Throwable exception = ExceptionUtil.handleInterruptions(e);
            log.error("Failed to read jar entry {}", entry.getName(), exception);
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

}
