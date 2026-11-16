package io.github.headlesshq.headlessmc.launcher.profile;

/**
 * Fired synchronously after a {@link Profile} has been moved or renamed by the {@link ProfileMover}.
 * Observers use this to carry over what they store under the name of a profile, e.g. its backups.
 * The move has already happened, an observer that throws does not undo it.
 *
 * @param before the profile before the move.
 * @param after  the stored profile after the move.
 * @see ProfileMoveEvent
 */
public record ProfileMovedEvent(Profile before, Profile after) {
    /**
     * @return {@code true} if the profile has another name now.
     */
    public boolean isRename() {
        return !before.name().equals(after.name());
    }

}
