package io.github.headlesshq.headlessmc.launcher.backup.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Arrays;
import java.util.Locale;

/**
 * The types of backups the {@link ProfileBackupProvider} supports.
 */
@RegisterForReflection
public enum ProfileBackupType implements ReflectionRegistered {
    /**
     * The entire game directory of a profile, together with the profile itself.
     */
    FULL,
    /**
     * All worlds of a profile.
     */
    WORLDS,
    /**
     * A single world of a profile.
     */
    WORLD;

    /**
     * @return the lowercase name of this type, as used by commands.
     */
    public String getName() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parses a backup type, ignoring case.
     *
     * @param name the name of the backup type.
     * @return the parsed backup type.
     * @throws IllegalArgumentException if no backup type has the given name.
     */
    public static ProfileBackupType parse(String name) {
        return Arrays.stream(values())
            .filter(type -> type.name().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown backup type '" + name + "', available: "
                    + String.join(", ", Arrays.stream(values()).map(ProfileBackupType::getName).toList())
            ));
    }

}
