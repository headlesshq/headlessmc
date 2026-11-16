package io.github.headlesshq.headlessmc.launcher.profile;

import java.nio.file.Path;

/**
 * Fired synchronously before a {@link Profile} is moved or renamed by the {@link ProfileMover},
 * after the move has been validated, but before anything has been changed.
 * An observer that throws prevents the move, e.g. because the profile is in use.
 * To react to a move that has happened, observe the {@link ProfileMovedEvent}.
 *
 * @param profile the profile as it is now.
 * @param newName the name the profile is going to have, the current name if it is not renamed.
 * @param newPath the game directory the profile is going to have, the current one if it is not moved.
 */
public record ProfileMoveEvent(Profile profile, String newName, Path newPath) {
    /**
     * @return {@code true} if the profile is going to get another name.
     */
    public boolean isRename() {
        return !profile.name().equals(newName);
    }

    /**
     * @return {@code true} if the game directory is going to be moved.
     */
    public boolean isMove() {
        return !profile.path().toAbsolutePath().normalize().equals(newPath.toAbsolutePath().normalize());
    }

}
