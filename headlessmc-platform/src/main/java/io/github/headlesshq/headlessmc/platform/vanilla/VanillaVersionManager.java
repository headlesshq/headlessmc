package io.github.headlesshq.headlessmc.platform.vanilla;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface VanillaVersionManager {
    List<VanillaVersion> getVersions();

    boolean isOlderThan(String version, String versionToCheck);

    default boolean isOlderThan(VanillaVersion version, VanillaVersion versionToCheck) {
        return isOlderThan(version.getName(), versionToCheck.getName());
    }

    default @Nullable VanillaVersion getVersion(String version) {
        return getVersions()
                .stream()
                .filter(vv -> vv.getName().equalsIgnoreCase(version))
                .findFirst()
                .orElse(null);
    }

}
