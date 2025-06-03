package io.github.headlesshq.headlessmc.launcher.specifics;

import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.launcher.version.family.FamilyUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * Information about a Minecraft version.
 */
@Data
@RequiredArgsConstructor
public class VersionInfo {
    private final String version;
    private final @Nullable Modlauncher modlauncher;

    public VersionInfo(Version version) {
        Version vanilla = FamilyUtil.getOldestParent(version); // TODO: use parent name instead?
        this.version = vanilla.getName();
        this.modlauncher = Modlauncher.getFromVersionName(version.getName());
    }

    public String getDescription() {
        return version + (modlauncher == null ? "" : "-" + modlauncher.getHmcName());
    }

    public static VersionInfo requireModLauncher(Version version) throws VersionSpecificException {
        VersionInfo info = new VersionInfo(version);
        if (info.getModlauncher() == null) {
            throw new VersionSpecificException("Failed to find modlauncher for " + version.getName());
        }

        return info;
    }

}
