package io.github.headlesshq.headlessmc.launcher.server.downloader;

import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfo;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfoUtil;
import io.github.headlesshq.headlessmc.version.Version;

import java.io.IOException;

public class VersionUtil {
    public static Version getVersion(Launcher launcher, String version) throws IOException {
        VersionInfo versionInfo = launcher.getVersionInfoCache().getByName(version);
        if (versionInfo == null) {
            throw new IOException("Failed to find version '" + version + "'");
        }

        return VersionInfoUtil.toVersion(
                versionInfo, launcher.getVersionService(), launcher.getDownloadService());
    }

}
