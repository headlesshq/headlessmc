package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * What created a {@link Backup}.
 * Automatic backups are pruned automatically, manual backups only when asked to.
 */
@RegisterForReflection
public enum BackupOrigin implements ReflectionRegistered {
    /**
     * Created by the user with the backup command.
     */
    MANUAL,
    /**
     * Created automatically before a profile was launched, see {@link BackupConfig#auto()}.
     */
    LAUNCH,
    /**
     * Created automatically before a backup was restored, containing the files that were changed by the restore.
     */
    RESTORE;

    /**
     * The origins of backups that have been created automatically.
     */
    public static final Set<BackupOrigin> AUTOMATIC = Set.of(LAUNCH, RESTORE);

    /**
     * @return {@code true} if HeadlessMc created backups of this origin automatically.
     */
    public boolean isAutomatic() {
        return AUTOMATIC.contains(this);
    }

    /**
     * @return the lowercase name of this origin, as used by commands.
     */
    public String getName() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parses an origin, ignoring case.
     *
     * @param name the name of the origin.
     * @return the parsed origin.
     * @throws IllegalArgumentException if no origin has the given name.
     */
    public static BackupOrigin parse(String name) {
        return Arrays.stream(values())
            .filter(origin -> origin.name().equalsIgnoreCase(name.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown backup origin '" + name + "', available: "
                    + String.join(", ", Arrays.stream(values()).map(BackupOrigin::getName).toList())
            ));
    }

}
