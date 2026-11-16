package io.github.headlesshq.headlessmc.files;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Provides the {@code .minecraft} directories HeadlessMc works with.
 * There is always one <em>primary</em> directory ({@code hmc.files.mc}, by default the
 * {@code .minecraft} directory of the operating system) and any number of additional directories
 * ({@code hmc.files.mc-dirs}), e.g. on other disks.
 *
 * <p>A directory being "known" only means that it is searched when versions are listed or resolved.
 * Any other directory can still be installed into or launched from, use {@link #of(Path)} for these.</p>
 *
 * <p>{@link McFiles} cannot be injected, because there is no single {@code .minecraft} directory anymore.
 * Code that needs one has to decide which one it means:</p>
 * <ul>
 *     <li>{@link #require(Path)}: the directory a version.json is installed in.
 *     This is the one to use for everything that belongs to a version, like its libraries and assets.</li>
 *     <li>{@link #getPrimary()}: the default directory for new installations.</li>
 *     <li>{@link #all()}: when searching for something that is already installed.</li>
 * </ul>
 */
public interface McFilesService {
    /**
     * @return the primary {@code .minecraft} directory, the default for new installations.
     */
    McFiles getPrimary();

    /**
     * @return all known {@code .minecraft} directories without duplicates. The primary directory comes first,
     * the additional directories follow in the order they have been configured in.
     * Directories that are {@link McFiles#isAvailable() not available} are included.
     */
    List<McFiles> all();

    /**
     * Finds the known {@code .minecraft} directory a file is in.
     * If known directories are nested in each other, the innermost one is returned.
     *
     * @param file the file to find the directory of.
     * @return the known directory containing the file, or none if the file is not inside a known directory.
     */
    Optional<McFiles> find(Path file);

    /**
     * Gets {@link McFiles} for any directory.
     *
     * @param mcDir a {@code .minecraft} directory, known or not.
     * @return the known {@code McFiles} for that directory, or new ones if the directory is not known.
     */
    McFiles of(Path mcDir);

    /**
     * Gets the {@code .minecraft} directory a version.json belongs to, this never fails:
     * <ol>
     *     <li>the known directory {@link #find(Path) containing} the file,</li>
     *     <li>else, if the file is at {@code <dir>/versions/<id>/<id>.json}, that {@code <dir>},</li>
     *     <li>else the {@link #getPrimary() primary} directory.</li>
     * </ol>
     *
     * @param versionJson the version.json file, {@code null} if a version does not come from a file.
     * @return the directory to use for everything belonging to that version.
     */
    McFiles require(@Nullable Path versionJson);

    /**
     * Creates a service over a fixed list of directories.
     *
     * @param primary    the primary directory.
     * @param additional the additional directories.
     * @return a service for the given directories.
     */
    static McFilesService of(McFiles primary, McFiles... additional) {
        return new FixedMcFilesService(primary, List.of(additional));
    }

}
