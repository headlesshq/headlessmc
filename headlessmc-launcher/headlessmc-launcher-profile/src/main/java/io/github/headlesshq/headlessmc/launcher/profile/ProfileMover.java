package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Moves the game directory of a {@link Profile} and renames profiles.
 * The game directory of a profile is decided when the profile is created and nothing else changes it,
 * this is the way to relocate it, e.g. to another disk.
 *
 * <p>Everything is validated before anything is changed, and the profile is only written after its files
 * have been moved, so a move that fails leaves the profile and its old game directory as they were.
 * Only the game directory is moved. The version, libraries and assets a profile launches belong to a
 * {@code .minecraft} directory, not to the profile.</p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileMover {
    private final LaunchTargetService launchTargetService;
    private final ProfileLaunchTarget profileLaunchTarget;
    private final ProfileService profileService;
    private final McFilesService mcFilesService;
    private final FileService fileService;
    private final Event<ProfileMoveEvent> moveEvent;
    private final Event<ProfileMovedEvent> movedEvent;

    /**
     * Moves and/or renames a profile.
     *
     * @param request what to move where.
     * @return what has been done.
     * @throws IllegalArgumentException if the request cannot be fulfilled, nothing has been changed then.
     * @throws HeadlessMcException      if moving fails, or an observer of the {@link ProfileMoveEvent} prevents it.
     */
    public Result move(Request request) throws HeadlessMcException {
        Profile profile = request.profile();
        String newName = request.newName() == null ? profile.name() : request.newName();
        Path oldPath = normalize(profile.path());
        Path newPath = request.newPath() == null ? oldPath : normalize(request.newPath());
        boolean rename = !profile.name().equals(newName);
        boolean move = !oldPath.equals(newPath);
        if (!rename && !move) {
            throw new IllegalArgumentException(
                "Profile " + profile.name() + " already has that name and game directory, nothing to do."
            );
        }

        if (rename) {
            validateName(profile, newName, request.force());
        }

        List<Profile> shared = List.of();
        if (move) {
            shared = getProfilesSharing(profile, oldPath);
            validateMove(profile, oldPath, newPath, shared, request.withShared());
        }

        moveEvent.fire(new ProfileMoveEvent(profile, newName, newPath));
        for (Profile other : shared) {
            moveEvent.fire(new ProfileMoveEvent(other, other.name(), newPath));
        }

        boolean filesMoved = move && Files.exists(oldPath);
        if (filesMoved) {
            if (VersionDirectories.isVersionDirectory(oldPath) || VersionDirectories.isVersionDirectory(newPath)) {
                // a game directory that is also the directory of an installed version, see GameDirLayout:
                // the game moves, the version stays installed where it is
                moveGameFiles(oldPath, newPath);
            } else {
                moveDirectory(oldPath, newPath);
            }
        }

        Profile moved = move ? relocate(profile, newPath) : profile;
        moved = profileService.save(moved.withName(newName));
        List<Profile> updated = new ArrayList<>();
        if (rename) {
            // file names are case-insensitive, a profile that only changes its case keeps its file
            if (!profile.name().equalsIgnoreCase(newName)) {
                profileService.remove(profile);
            }

            for (Profile child : profileLaunchTarget.getChildren(profile.name())) {
                // a child that is also moved is written below, with its new path
                if (shared.stream().noneMatch(other -> other.name().equalsIgnoreCase(child.name()))) {
                    updated.add(profileService.save(profileLaunchTarget.withParent(child, newName)));
                }
            }
        }

        for (Profile other : shared) {
            Profile relocated = relocate(other, newPath);
            if (rename && profileLaunchTarget.getParentName(relocated).filter(profile.name()::equalsIgnoreCase).isPresent()) {
                relocated = profileLaunchTarget.withParent(relocated, newName);
            }

            updated.add(profileService.save(relocated));
        }

        log.info("Moved profile {} ({}) to {} ({})", profile.name(), oldPath, newName, newPath);
        try {
            movedEvent.fire(new ProfileMovedEvent(profile, moved));
        } catch (RuntimeException e) {
            // the profile has been moved, an observer cannot change that anymore
            log.error("Profile {} has been moved, but something depending on it could not follow", newName, e);
        }

        return new Result(profile, moved, filesMoved, List.copyOf(updated));
    }

    /**
     * @param profile a profile.
     * @return the other stored profiles that have the same game directory as the given profile.
     */
    public List<Profile> getProfilesSharing(Profile profile) {
        return getProfilesSharing(profile, normalize(profile.path()));
    }

    private List<Profile> getProfilesSharing(Profile profile, Path path) {
        return profileService.getProfiles().stream()
            .filter(other -> !other.name().equalsIgnoreCase(profile.name()))
            .filter(other -> normalize(other.path()).equals(path))
            .toList();
    }

    private void validateName(Profile profile, String newName, boolean force) {
        if (newName.isBlank()) {
            throw new IllegalArgumentException("The new name of profile " + profile.name() + " is empty.");
        }

        if (!profile.name().equalsIgnoreCase(newName) && !force && profileService.getProfile(newName).isPresent()) {
            throw new IllegalArgumentException("Profile " + newName + " already exists, use --force to overwrite it.");
        }
    }

    private void validateMove(Profile profile, Path oldPath, Path newPath, List<Profile> shared, boolean withShared) {
        if (newPath.startsWith(oldPath)) {
            throw new IllegalArgumentException("Cannot move " + oldPath + " into itself (" + newPath + ").");
        }

        if (oldPath.startsWith(newPath)) {
            throw new IllegalArgumentException("Cannot move " + oldPath + " to " + newPath + ", which contains it.");
        }

        for (McFiles mcFiles : mcFilesService.all()) {
            Path mcDir = normalize(mcFiles.getMcDir());
            if (mcDir.startsWith(oldPath)) {
                throw new IllegalArgumentException(
                    "The game directory " + oldPath + " of profile " + profile.name() + " "
                        + (mcDir.equals(oldPath) ? "is" : "contains") + " the Minecraft directory " + mcDir
                        + ", it cannot be moved."
                );
            }
        }

        if (!shared.isEmpty() && !withShared) {
            throw new IllegalArgumentException(
                "The game directory " + oldPath + " of profile " + profile.name() + " is also used by "
                    + String.join(", ", shared.stream().map(Profile::name).toList())
                    + ". Use --with-shared to move it for all of them."
            );
        }

        if (Files.exists(newPath)) {
            if (!Files.isDirectory(newPath)) {
                throw new IllegalArgumentException(newPath + " is not a directory.");
            }

            if (Files.exists(oldPath)) {
                try {
                    // the directory of an installed version is a free game directory if nothing is played in it yet
                    boolean empty = VersionDirectories.isVersionDirectory(newPath)
                        ? VersionDirectories.getGameFiles(newPath).isEmpty()
                        : isEmpty(newPath);
                    if (!empty) {
                        throw new IllegalArgumentException("The directory " + newPath + " is not empty.");
                    }
                } catch (IOException e) {
                    throw new FileException("Failed to list " + newPath, e);
                }
            }
        }
    }

    private Profile relocate(Profile profile, Path newPath) {
        LaunchTargetHandle target = profile.target();
        try {
            target = launchTargetService.getTarget(target.name())
                .map(launchTarget -> launchTarget.relocate(profile, newPath))
                .orElse(target);
        } catch (HeadlessMcException e) {
            log.warn("Failed to move the launch target of profile {}: {}", profile.name(), e.getMessage());
        }

        return profile.withPath(newPath).withTarget(target);
    }

    /**
     * Moves a directory, as a rename if possible. Across file stores the directory is copied,
     * and the source is only deleted once everything has been copied.
     */
    private void moveDirectory(Path source, Path destination) throws FileException {
        try {
            @Nullable Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.isDirectory(destination)) {
                // validated to be empty, a rename needs it to not exist
                Files.delete(destination);
            }

            try {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (AtomicMoveNotSupportedException e) {
                log.debug("Cannot rename {} to {}, copying it", source, destination, e);
            }
        } catch (IOException e) {
            throw new FileException("Failed to move " + source + " to " + destination, e);
        }

        try {
            copyDirectory(source, destination);
        } catch (IOException | RuntimeException e) {
            try {
                fileService.delete(destination);
            } catch (FileException deleteException) {
                e.addSuppressed(deleteException);
            }

            throw new FileException(
                "Failed to copy " + source + " to " + destination + ", nothing has been moved", e
            );
        }

        try {
            fileService.delete(source);
        } catch (FileException e) {
            // everything is at the destination, the profile can be moved, only the old files are left over
            log.warn("Moved {} to {}, but failed to delete the old directory: {}", source, destination, e.getMessage());
        }
    }

    /**
     * Moves the files of a game out of, or into, the directory of an installed version:
     * everything but the files of the version, see {@link VersionDirectories}.
     * If something cannot be moved, what has been moved is moved back.
     */
    private void moveGameFiles(Path source, Path destination) throws FileException {
        List<Path> files;
        try {
            files = VersionDirectories.isVersionDirectory(source)
                ? VersionDirectories.getGameFiles(source)
                : listAll(source);
            Files.createDirectories(destination);
        } catch (IOException e) {
            throw new FileException("Failed to move " + source + " to " + destination, e);
        }

        List<Path> moved = new ArrayList<>();
        try {
            for (Path file : files) {
                moveEntry(file, destination.resolve(file.getFileName().toString()));
                moved.add(file);
            }
        } catch (IOException | RuntimeException e) {
            for (Path file : moved.reversed()) {
                try {
                    moveEntry(destination.resolve(file.getFileName().toString()), file);
                } catch (IOException | RuntimeException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
            }

            throw new FileException(
                "Failed to move the files in " + source + " to " + destination + ", nothing has been moved", e
            );
        }

        try {
            if (!VersionDirectories.isVersionDirectory(source) && isEmpty(source)) {
                Files.delete(source);
            }
        } catch (IOException e) {
            log.warn("Moved {} to {}, but failed to delete the old directory: {}", source, destination, e.getMessage());
        }
    }

    /**
     * Moves a file or directory, as a rename if possible, otherwise by copying it.
     */
    private void moveEntry(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (AtomicMoveNotSupportedException e) {
            log.debug("Cannot rename {} to {}, copying it", source, destination, e);
        }

        if (Files.isDirectory(source)) {
            copyDirectory(source, destination);
        } else {
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
        }

        fileService.delete(source);
    }

    private static List<Path> listAll(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.sorted().toList();
        }
    }

    private static boolean isEmpty(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.findAny().isEmpty();
        }
    }

    /**
     * Copies a directory and verifies that every file arrived with its size.
     */
    void copyDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(destination.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(source.relativize(file).toString());
                Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                if (attrs.isRegularFile() && Files.size(target) != attrs.size()) {
                    throw new IOException("Size of " + target + " differs from " + file);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    /**
     * What to move where.
     *
     * @param profile    the stored profile to move.
     * @param newPath    the new game directory, {@code null} to keep the game directory.
     * @param newName    the new name of the profile, {@code null} to keep the name.
     * @param withShared also moves the game directory for other profiles using it,
     *                   without it a game directory that is used by other profiles is not moved.
     * @param force      overwrites a profile that already has the new name.
     */
    public record Request(
        Profile profile,
        @Nullable Path newPath,
        @Nullable String newName,
        boolean withShared,
        boolean force
    ) {

    }

    /**
     * What has been done by a move.
     *
     * @param before     the profile before the move.
     * @param after      the stored profile after the move.
     * @param filesMoved whether files have been moved, {@code false} if the profile has only been renamed,
     *                   or if its game directory did not exist yet.
     * @param updated    other profiles that have been changed: profiles inheriting from a renamed profile
     *                   and profiles sharing the moved game directory.
     */
    public record Result(Profile before, Profile after, boolean filesMoved, List<Profile> updated) {

    }

}
