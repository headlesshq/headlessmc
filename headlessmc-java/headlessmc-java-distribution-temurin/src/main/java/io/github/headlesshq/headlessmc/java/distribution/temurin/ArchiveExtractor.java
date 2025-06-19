package io.github.headlesshq.headlessmc.java.distribution.temurin;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.CustomLog;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@CustomLog
@ApplicationScoped
public class ArchiveExtractor {
    public void extract(Path path, boolean toParentDir) throws IOException {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".zip")) {
            if (toParentDir) {
                unzip(path, path.getParent());
            } else {
                String dirName = fileName.substring(0, fileName.lastIndexOf("."));
                unzip(path, path.getParent().resolve(dirName));
            }
        } else if (fileName.endsWith(".tar.gz")) {
            if (toParentDir) {
                untar(path, path.getParent());
            } else {
                String dirName = fileName.substring(0, fileName.length() - ".tar.gz".length());
                untar(path, path.getParent().resolve(dirName));
            }
        } else {
            throw new IOException("Failed to extract " + path + ", unknown format");
        }
    }

    private void unzip(Path from, Path to) throws IOException {
        log.debug("Extracting zip " + from.toAbsolutePath() + " to " + to.toAbsolutePath());
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(from))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = to.resolve(entry.getName());
                if (entry.isDirectory()) {
                    if (!Files.exists(entryPath)) {
                        Files.createDirectories(entryPath);
                    }
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try {
                        Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ioe) {
                        zipInputStream.closeEntry();
                        throw ioe;
                    }
                }

                zipInputStream.closeEntry();
            }
        }
    }

    private void untar(Path from, Path to) throws IOException {
        log.debug("Extracting tar " + from.toAbsolutePath() + " to " + to.toAbsolutePath());
        try (GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(Files.newInputStream(from));
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            TarArchiveEntry entry;

            while ((entry = tarIn.getNextEntry()) != null) {
                Path entryPath = to.resolve(entry.getName());
                if (entry.isDirectory()) {
                    if (!Files.exists(entryPath)) {
                        Files.createDirectories(entryPath);
                    }
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(tarIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

}
