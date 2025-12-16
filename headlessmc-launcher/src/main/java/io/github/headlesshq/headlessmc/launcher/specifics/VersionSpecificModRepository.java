package io.github.headlesshq.headlessmc.launcher.specifics;

import io.github.headlesshq.headlessmc.api.HasName;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import lombok.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a repository that provides downloads for a version specific mod, e.g. the hmc-specifics.
 *
 * @see VersionSpecificMods
 * @see <a href=https://github.com/headlesshq/hmc-specifics>https://github.com/headlesshq/hmc-specifics</a>
 * @see <a href=https://github.com/headlesshq/mc-runtime-test>https://github.com/headlesshq/mc-runtime-test</a>
 * @see <a href=https://github.com/headlesshq/hmc-optimizations>https://github.com/headlesshq/hmc-optimizations</a>
 */
@Data
public class VersionSpecificModRepository implements HasName {
    @Getter(AccessLevel.NONE)
    private volatile GithubReleaseService.GithubRelease latestRelease;
    @Getter(AccessLevel.NONE)
    private final Object lock = new Object();

    /**
     * The URL to download this version specific mod from.
     * This could be some a maven URL that points to the package the mod is in, or a github release url.
     * <p>E.g. <a href=https://github.com/headlesshq/hmc-specifics/releases/download/>
     * https://github.com/headlesshq/hmc-specifics/releases/download/</a>
     */
    @EqualsAndHashCode.Exclude
    private final URL url;
    /**
     * The owner of this Github repository.
     */
    private final String owner;
    /**
     * The name of the version specific mod.
     * <p>E.g. hmc-specifics
     */
    private final String name;
    /**
     * A fallback version if GithubReleases cannot be fetched.
     */
    private final String fallbackVersion;
    /**
     * An appendix for the jar files.
     * <p>E.g. -release
     */
    private final String appendix;

    /**
     * Returns the filename for the version specific mod for a specific mc version and a ModLauncher.
     * This is done by adding name, mcversion, version, modlauncher.getHmcName, appendix and .jar together.
     * <p>E.g. hmc-specifics-1.12.2-2.0.0-fabric-release.jar
     *
     * @param versionInfo the versionInfo containing the version and modlauncher.
     * @return the filename for the version specific mod release for the specified version and modlauncher.
     */
    public String getFileName(String version, VersionInfo versionInfo) {
        return name + "-" + versionInfo.getVersion() + "-" + version + "-"
                + Objects.requireNonNull(versionInfo.getModlauncher(), "modlauncher was null").getHmcName()
                + appendix + ".jar";
    }

    /**
     * Returns the download URL for a release of the version specific mod.
     * This is done by concatenating the url, version number and getFileName.
     * <p>E.g. <a href=https://github.com/headlesshq/hmc-specifics/releases/download/2.0.0/hmc-specifics-1.12.2-2.0.0-fabric-release.jar>
     * https://github.com/headlesshq/hmc-specifics/releases/download/2.0.0/hmc-specifics-1.12.2-2.0.0-fabric-release.jar</a>
     *
     * @param versionInfo the versionInfo containing the version and modlauncher.
     * @return the download URL for the version specific mod release for the specified version and modlauncher.
     * @throws MalformedURLException if the resulting URL would be malformed.
     */
    public URL getDownloadURL(String version, VersionInfo versionInfo) throws MalformedURLException {
        return new URL(url + version + "/" + getFileName(version, versionInfo));
    }

    /**
     * Returns a {@link Pattern} that matches file names following the scheme of this repository.
     *
     * @return a pattern to check if a filename came from this repository.
     */
    public Pattern getFileNamePattern() {
        return Pattern.compile(name + "-.*-.*-.*" + appendix + ".jar");
    }

    /**
     * Legacy {@link #getVersion(DownloadService)}.
     *
     * @return the tag name of the latest GitHub release version of this repository.
     * @deprecated use {@link #getVersion(DownloadService)} instead.
     */
    @Deprecated
    @SneakyThrows
    public String getVersion() {
        return getVersion(new DownloadService());
    }

    /**
     * Caches and downloads the current latest release for this VersionSpecificModRepository.
     *
     * @param downloadService the service to use to GET the latest release
     * @return the tag_name of the latest GitHub Release of this repository
     * @throws IOException if something goes wrong while fetching the latest GitHub Release.
     */
    public String getVersion(DownloadService downloadService) throws IOException {
        if (latestRelease == null) {
            synchronized (lock) {
                if (latestRelease == null) {
                    GithubReleaseService releaseService = new GithubReleaseService(downloadService);
                    latestRelease = releaseService.getGithubRelease(owner, name);
                }
            }
        }

        return latestRelease == null ? fallbackVersion : latestRelease.tag_name;
    }

}
