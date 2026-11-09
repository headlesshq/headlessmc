package io.github.headlesshq.headlessmc.java.distribution.foojay;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Extracts archives downloaded from foojay.
 * The archive and compressor streams are created directly instead of via the
 * commons-compress stream factories, because the factories reference every
 * supported format, including ones whose optional dependencies (e.g. brotli, zstd)
 * are not on the classpath, which breaks the native image build.
 *
 * @param archiver     the archive format, {@code tar} or {@code zip}.
 * @param decompressor the compression format, {@code gz}, {@code z} or {@code null} for none.
 */
record Extractor(String archiver, @Nullable String decompressor) {
    public void extract(Path destination, InputStream inputStream) throws IOException {
        Files.createDirectories(destination);
        try (ArchiveInputStream<?> ais = createArchiveInputStream(decompress(new BufferedInputStream(inputStream)))) {
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

    private InputStream decompress(InputStream in) throws IOException {
        if (decompressor == null) {
            return in;
        }

        return switch (decompressor) {
            case "gz" -> new GzipCompressorInputStream(in);
            case "z" -> new ZCompressorInputStream(in);
            default -> throw new IOException("Unsupported compressor: " + decompressor);
        };
    }

    private ArchiveInputStream<?> createArchiveInputStream(InputStream in) throws IOException {
        return switch (archiver) {
            case "tar" -> new TarArchiveInputStream(in);
            case "zip" -> new ZipArchiveInputStream(in) {
                @Override
                protected InputStream createZstdInputStream(InputStream in) throws IOException {
                    throw new IOException("Zstd compressed zip entries are not supported");
                }
            };
            default -> throw new IOException("Unsupported archiver: " + archiver);
        };
    }

}
