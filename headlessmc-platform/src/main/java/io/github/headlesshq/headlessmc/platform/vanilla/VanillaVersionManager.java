package io.github.headlesshq.headlessmc.platform.vanilla;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface VanillaVersionManager {
    List<VanillaVersion> getVersions();

    boolean isOlderThan(String version, String versionToCheck);

    default @Nullable VanillaVersion getVersion(String version) {
        return getVersions()
                .stream()
                .filter(vv -> vv.getName().equalsIgnoreCase(version))
                .findFirst()
                .orElse(null);
    }

}
