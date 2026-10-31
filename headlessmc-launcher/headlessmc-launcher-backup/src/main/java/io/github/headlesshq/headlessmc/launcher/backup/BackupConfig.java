package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;

/**
 * Configuration of the {@link BackupService}.
 */
@ConfigMapping(prefix = BackupConfig.PREFIX)
public interface BackupConfig extends DynamicConfig {
    /**
     * The prefix of all properties of this config.
     */
    String PREFIX = "hmc.backup";

    /**
     * The directory backups are placed in.
     * Corresponds to the {@code hmc.backup.dir} property.
     *
     * @return the backup directory, or an empty {@link Optional} to use the default.
     */
    Optional<String> dir();

    /**
     * Glob patterns of paths that are left out of backups, relative to the game directory.
     * Corresponds to the {@code hmc.backup.ignore} property.
     * {@code *} matches any characters except {@code /}, {@code **} matches across directories
     * and {@code ?} matches a single character. If a pattern matches a directory,
     * everything inside of it is ignored, e.g. {@code logs} or {@code **}{@code /*.log}.
     *
     * @return the patterns of paths to ignore.
     */
    @WithDefault("logs,crash-reports")
    List<String> ignore();

    /**
     * What to back up automatically before a profile is launched:
     * {@code none}, {@code worlds} or {@code full}.
     * Corresponds to the {@code hmc.backup.auto} property.
     * No backup is created if nothing changed since the last backup of the same type.
     *
     * @return what to back up before launching.
     */
    @WithDefault("none")
    String auto();

    /**
     * How many automatically created backups to keep for each profile,
     * older ones are deleted automatically. Manually created backups are never deleted automatically.
     * Corresponds to the {@code hmc.backup.auto-keep} property, {@code 0} disables automatic pruning.
     *
     * @return how many automatic backups to keep.
     */
    @WithDefault("5")
    int autoKeep();

}
