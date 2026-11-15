package io.github.headlesshq.headlessmc.files;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@code .minecraft} directory, the place versions, libraries and assets are installed in.
 * HeadlessMc can work with multiple of these directories, e.g. on multiple disks, see {@link McFilesService}.
 * A {@code .minecraft} directory has no name, it is identified by its path:
 * two {@link McFiles} are {@link #isSame(McFiles) the same} if they point to the same directory.
 *
 * <p>Everything a launch needs, the version.json, libraries, assets, logging configs and the client jar,
 * comes from one single {@code McFiles}, the one the launched version.json is installed in.
 * Nothing is shared between two {@code .minecraft} directories.</p>
 *
 * @see McFilesService
 */
public interface McFiles {
    /**
     * @return the {@code .minecraft} directory.
     */
    Path getMcDir();

    /**
     * @return the directory version.json files and client jars are placed in, {@code <mcDir>/versions}.
     */
    default Path getVersionsDir() {
        return getMcDir().resolve("versions");
    }

    /**
     * @return the directory libraries are placed in, {@code <mcDir>/libraries}.
     */
    default Path getLibraryDir() {
        return getMcDir().resolve("libraries");
    }

    /**
     * @return the directory assets are placed in, {@code <mcDir>/assets}.
     */
    default Path getAssetsDir() {
        return getMcDir().resolve("assets");
    }

    /**
     * @return the directory the resources of very old versions are placed in, {@code <mcDir>/resources}.
     */
    default Path getResourcesDir() {
        return getMcDir().resolve("resources");
    }

    /**
     * A {@code .minecraft} directory can be configured, but not be there,
     * e.g. because the disk it is on is currently not mounted.
     *
     * @return {@code true} if {@link #getMcDir()} is an existing directory.
     */
    default boolean isAvailable() {
        return Files.isDirectory(getMcDir());
    }

    /**
     * @param other the {@code McFiles} to compare this one to.
     * @return {@code true} if the other {@code McFiles} points to the same directory.
     */
    default boolean isSame(McFiles other) {
        return normalize(getMcDir()).equals(normalize(other.getMcDir()));
    }

    /**
     * Creates {@link McFiles} for any directory.
     * The directory does not need to exist, or to be one of the directories known to the {@link McFilesService}.
     *
     * @param mcDir the {@code .minecraft} directory.
     * @return {@code McFiles} for the given directory, which are equal to all other
     * {@code McFiles} created by this method for the same directory.
     */
    static McFiles of(Path mcDir) {
        return new PathMcFiles(mcDir);
    }

    /**
     * @param path the path to normalize.
     * @return the absolute, normalized form of the path, which {@code .minecraft} directories are compared by.
     */
    static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

}
