package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@ConfigMapping(prefix = "hmc.files")
public interface FileConfig extends DynamicConfig {
    Optional<String> location();

    @WithDefault("true")
    boolean gameForEachVersion();

    /**
     * Where the default game directory of a client is placed:
     * <ul>
     *     <li>{@code headlessmc}: a directory named after the version in the {@code .minecraft} directory,
     *     or that directory itself, see {@link #gameForEachVersion()}.</li>
     *     <li>{@code versions}: the directory the version is installed in, {@code versions/<id>},
     *     like HMCL and PCL2 do with version isolation.</li>
     *     <li>{@code versions-modded}: like {@code versions} for modded versions, vanilla versions share the
     *     {@code .minecraft} directory, the default of HMCL.</li>
     * </ul>
     *
     * @return the name of the layout of game directories.
     */
    @WithDefault("headlessmc")
    String gameDirLayout();

    @WithName("mc")
    Optional<String> mcDir();

    /**
     * Additional {@code .minecraft} directories, e.g. on other disks.
     * Versions installed in these directories can be listed and launched like versions in {@link #mcDir()},
     * which stays the default for new installations.
     *
     * @return the paths of additional {@code .minecraft} directories.
     */
    @WithName("mc-dirs")
    Optional<List<String>> mcDirs();

    /**
     * Whether the drives of this computer are searched for {@code .minecraft} directories,
     * which are used like the directories listed in {@link #mcDirs()}, see {@link McDirDetector}.
     *
     * @return {@code true} to search for {@code .minecraft} directories on other drives.
     */
    @WithName("mc-detect")
    @WithDefault("false")
    boolean mcDetect();

    /**
     * @return how deep a drive is searched for {@code .minecraft} directories if {@link #mcDetect()} is enabled,
     * 1 only finds a directory at the top of a drive.
     */
    @WithName("mc-detect-depth")
    @WithDefault("3")
    int mcDetectDepth();

    @WithName("game")
    Optional<String> gameDir();

    default Optional<String> property(
        Optional<String> property,
        Optional<String> legacy,
        Consumer<String> warning,
        String legacyName,
        String name
    ) {
        if (legacy.isEmpty()) {
            return property;
        }

        if (property.isEmpty()) {
            warning.accept("Legacy configuration " + legacyName + " used, consider using " + name + " instead.");
            return legacy;
        }

        throw new ConfigException("Both " + name + " and deprecated configuration " + legacyName + " were used.");
    }

}
