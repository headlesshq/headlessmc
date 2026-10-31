package io.github.headlesshq.headlessmc.launcher.backup;

/**
 * Thrown if files cannot be backed up or restored because they are in use, e.g. by a running game.
 */
public class BackupInUseException extends IllegalStateException {
    public BackupInUseException(String message) {
        super(message);
    }

}
