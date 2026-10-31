package io.github.headlesshq.headlessmc.plugins;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * File operations needed to prepare plugin builds.
 */
@UtilityClass
class PluginFiles {
    /**
     * Copies a directory recursively.
     *
     * @param source the directory to copy.
     * @param target the directory to copy to, created if necessary.
     * @throws IOException if copying fails.
     */
    static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Extracts a zip file.
     *
     * @param zip    the zip file.
     * @param target the directory to extract into.
     * @throws IOException if extracting fails or an entry would be placed outside the target.
     */
    static void unzip(Path zip, Path target) throws IOException {
        Path root = target.toAbsolutePath().normalize();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path file = root.resolve(entry.getName()).normalize();
                if (!file.startsWith(root)) {
                    throw new IOException("Zip entry " + entry.getName() + " is outside of " + root);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(file);
                } else {
                    Files.createDirectories(file.getParent());
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

}
