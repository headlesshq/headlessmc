package io.github.headlesshq.headlessmc.launcher.specifics;

import lombok.Data;
import io.github.headlesshq.headlessmc.api.HasName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a repository that provides downloads for a version specific mod, e.g. the hmc-specifics.
 * @see VersionSpecificMods
 * @see <a href=https://github.com/headlesshq/hmc-specifics>https://github.com/headlesshq/hmc-specifics</a>
 * @see <a href=https://github.com/headlesshq/mc-runtime-test>https://github.com/headlesshq/mc-runtime-test</a>
 * @see <a href=https://github.com/headlesshq/hmc-optimizations>https://github.com/headlesshq/hmc-optimizations</a>
 */
@Data
public class VersionSpecificModRepository implements HasName {
    /**
     * The URL to download this version specific mod from.
     * This could be some a maven URL that points to the package the mod is in, or a github release url.
     * <p>E.g. <a href=https://github.com/headlesshq/hmc-specifics/releases/download/>
     * https://github.com/headlesshq/hmc-specifics/releases/download/</a>
     */
    private final URL url;
    /**
     * The name of the version specific mod.
     * <p>E.g. hmc-specifics
     */
    private final String name;
    /**
     * The release version of the mod (not the mc version).
     * <p>E.g. 2.0.0
     */
    private final String version;
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
    public String getFileName(VersionInfo versionInfo) {
        return name + "-" + versionInfo.getVersion() + "-" + version + "-"
            + Objects.requireNonNull(versionInfo.getModlauncher(), "modlauncher was null").getHmcName()
            + appendix + ".jar";
    }

    /**
     * Returns the download URL for a release of the version specific mod.
     * This is done by concatenating the url, version number and getFileName.
     * <p>E.g. <a href=https://github.com/headlesshq/hmc-specifics/releases/download/2.0.0/hmc-specifics-1.12.2-2.0.0-fabric-release.jar>
     *     https://github.com/headlesshq/hmc-specifics/releases/download/2.0.0/hmc-specifics-1.12.2-2.0.0-fabric-release.jar</a>
     *
     * @param versionInfo the versionInfo containing the version and modlauncher.
     * @return the download URL for the version specific mod release for the specified version and modlauncher.
     * @throws MalformedURLException if the resulting URL would be malformed.
     */
    public URL getDownloadURL(VersionInfo versionInfo) throws MalformedURLException {
        return new URL(url + version + "/" + getFileName(versionInfo));
    }

    /**
     * Returns a {@link Pattern} that matches file names following the scheme of this repository.
     *
     * @return a pattern to check if a filename came from this repository.
     */
    public Pattern getFileNamePattern() {
        return Pattern.compile(name + "-.*-.*-.*" + appendix + ".jar");
    }

}
