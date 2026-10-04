package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LibraryExtractor {
    public void extract(
        Version.Library library,
        Version.Extract extract,
        Path file,
        Path destination
    ) throws HeadlessMcException {
        List<String> excludes = extract.getExclude();
        if (excludes == null) {
            excludes = List.of();
        }

        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (excludes.stream().anyMatch(exclude -> entry.getName().startsWith(exclude))) {
                    continue;
                }

                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) {
                    throw new HeadlessMcIOException("Zip slip detected: %s, in file %s in library %s".formatted(
                        entry.getName(),
                        file,
                        library
                    ));
                }

                if (target.toString().toLowerCase(Locale.ENGLISH).endsWith(".class")) {
                    log.error("Extracting class file {} from {}", target, library.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

}
