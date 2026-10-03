package io.github.headlesshq.headlessmc.java.distribution.foojay;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

record Extractor(String archiver, @Nullable String decompressor) {
    public void extract(Path destination, InputStream inputStream) throws IOException {
        Files.createDirectories(destination);
        InputStream in = new BufferedInputStream(inputStream); // to support mark
        if (decompressor != null) {
            in = new CompressorStreamFactory()
                .createCompressorInputStream(decompressor, inputStream);
        }

        try (ArchiveInputStream<?> ais = new ArchiveStreamFactory().createArchiveInputStream(archiver, in)) {
            ArchiveEntry entry;
            while ((entry = ais.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) {
                    throw new IOException("Zip slip detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(ais, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

}
