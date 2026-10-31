package io.github.headlesshq.headlessmc.launcher.profile;

/**
 * Fired synchronously before a {@link Profile} is launched.
 * An observer that throws prevents the launch.
 *
 * @param profile    the profile that is about to be launched.
 * @param autoBackup {@code false} if the user asked to skip automatic backups for this launch.
 */
public record ProfileLaunchEvent(Profile profile, boolean autoBackup) {

}
