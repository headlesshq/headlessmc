package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.exceptions.FileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Provides the .minecraft folders HeadlessMc works with.
 * The primary folder is configured via {@code hmc.files.mc}, or the default .minecraft folder of the OS.
 * Additional folders, e.g. on other drives, can be configured via {@code hmc.files.mc-folders}.
 * Versions are searched for in all folders, but each version is launched with the files of its own folder.
 */
public interface McFilesService {
    /**
     * @return the primary .minecraft folder.
     */
    McFiles getPrimaryMcFiles();

    /**
     * @return all .minecraft folders, starting with the {@link #getPrimaryMcFiles() primary} one,
     * followed by the additional folders in the order they were configured in.
     */
    List<McFiles> getAllMcFiles();

    /**
     * Finds the .minecraft folder that contains the given path.
     * If folders are nested, the innermost one is returned.
     *
     * @param path the path to find the .minecraft folder for.
     * @return the .minecraft folder containing the path.
     */
    default Optional<McFiles> getMcFiles(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return getAllMcFiles()
            .stream()
            .filter(mcFiles -> normalized.startsWith(mcFiles.getMcDir().toAbsolutePath().normalize()))
            .max(Comparator.comparingInt(mcFiles -> mcFiles.getMcDir().toAbsolutePath().normalize().getNameCount()));
    }

    /**
     * A version is always launched with the files of a single .minecraft folder,
     * as e.g. {@code ${library_directory}} can only point to one folder.
     * But files missing in that folder, like libraries and assets, can be copied from other folders
     * instead of downloading them again.
     *
     * @param file  the missing file, inside one of the .minecraft folders.
     * @param valid checks if the file found in another folder can be used, e.g. by verifying its hash.
     * @return {@code true} if the file has been copied from another folder.
     * @throws FileException if the file could not be copied.
     */
    default boolean copyFromOtherMcFiles(Path file, Predicate<Path> valid) throws FileException {
        Optional<McFiles> owner = getMcFiles(file);
        if (owner.isEmpty() || Files.exists(file)) {
            return false;
        }

        Path relative = owner.get().getMcDir().toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize());
        for (McFiles other : getAllMcFiles()) {
            Path candidate = other.getMcDir().toAbsolutePath().normalize().resolve(relative);
            if (other.equals(owner.get()) || !Files.isRegularFile(candidate) || !valid.test(candidate)) {
                continue;
            }

            try {
                Files.createDirectories(file.getParent());
                Files.copy(candidate, file, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                throw new FileException("Failed to copy " + candidate + " to " + file, e);
            }
        }

        return false;
    }

}
