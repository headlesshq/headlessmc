package io.github.headlesshq.headlessmc.launcher.version;

import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.util.Table;
import org.semver4j.Semver;

import java.util.Collection;
import java.util.stream.Collectors;

@CustomLog
@UtilityClass
public final class VersionUtil {
    public static Collection<Version> releases(Collection<Version> versions) {
        return versions.stream()
                       .filter(v -> "release".equalsIgnoreCase(v.getType()))
                       .collect(Collectors.toList());
    }

    public static String makeTable(Collection<Version> versions) {
        return new Table<Version>()
            .withColumn("id", v -> String.valueOf(v.getId()))
            .withColumn("name", HasName::getName)
            .withColumn("parent", v -> v.getParent() == null ? "" : v.getParent().getName())
            .addAll(versions)
            .build();
    }

    public static boolean isOlderThanSafe(String versionToTest, String versionPotentiallyYounger) {
        try {
            return isOlderThan(versionToTest, versionPotentiallyYounger);
        } catch (IllegalArgumentException e) {
            log.error(e);
            return false;
        }
    }

    public static boolean isOlderThan(String versionToTest, String versionPotentiallyYounger) throws IllegalArgumentException {
        try {
            Semver semver1 = Semver.coerce(versionToTest);
            Semver semver2 = Semver.coerce(versionPotentiallyYounger);
            assert semver1 != null;
            assert semver2 != null;
            return semver1.compareTo(semver2) < 0;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse semver '" + versionToTest + "' or '" + versionPotentiallyYounger + "'", e);
        }
    }

}
