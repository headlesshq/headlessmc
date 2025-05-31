package io.github.headlesshq.headlessmc.version;

import lombok.Cleanup;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@CustomLog
@RequiredArgsConstructor
final class ExtractorImpl implements Extractor {
    private final List<String> exclusions;

    @Override
    public void extract(Path from, Path to) throws IOException {
        log.debug("Extracting " + from + " to " + to);
        @Cleanup
        JarFile jar = new JarFile(from.toFile());
        Enumeration<JarEntry> enumeration = jar.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry je = enumeration.nextElement();
            if (shouldExtract(je.getName()) && !je.isDirectory()) {
                log.debug(String.format(
                        "Extracting  : %s from %s to %s%s%s",
                        je.getName(), jar.getName(),
                        to, File.separator,
                        je.getName()
                    )
                );
                @Cleanup
                InputStream is = jar.getInputStream(je);
                @Cleanup
                OutputStream os = Files.newOutputStream(to.resolve(je.getName()));
                IOUtil.copy(is, os);
            }
        }
    }

    @Override
    public boolean shouldExtract(@Nullable String name) {
        return name != null && exclusions.stream().noneMatch(name::startsWith);
    }

    @Override
    public boolean isExtracting() {
        return true;
    }

}
