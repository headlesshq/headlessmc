package io.github.headlesshq.headlessmc.launcher.profile;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Where the default game directory of a client is placed, configured with {@code hmc.files.game-dir-layout}.
 * Launchers disagree on this, and a game directory holds what a user cares about: worlds, mods and settings.
 * To use the {@code .minecraft} directory of another launcher, HeadlessMc has to look for them in the same place.
 *
 * @see GameDirService
 */
public enum GameDirLayout {
    /**
     * The layout of HeadlessMc: a directory named after the version in the {@code .minecraft} directory,
     * e.g. {@code .minecraft/fabric-1.21.1}, or the {@code .minecraft} directory itself if
     * {@code hmc.files.game-for-each-version} is disabled.
     */
    HEADLESSMC("headlessmc"),
    /**
     * The game directory of a version is the directory its version.json is installed in,
     * e.g. {@code .minecraft/versions/fabric-loader-0.16.9-1.21.1}.
     * This is the layout of HMCL and PCL2 with version isolation for all versions.
     */
    VERSIONS("versions"),
    /**
     * Like {@link #VERSIONS} for modded versions, vanilla versions share the {@code .minecraft} directory.
     * This is the default of HMCL.
     */
    VERSIONS_MODDED("versions-modded");

    private final String configName;

    GameDirLayout(String configName) {
        this.configName = configName;
    }

    /**
     * @return the value of {@code hmc.files.game-dir-layout} for this layout.
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * @return {@code true} if game directories are placed in the directory of an installed version,
     * so that the default game directory of a version is only known once the version is installed.
     */
    public boolean isInVersionDirectory() {
        return this != HEADLESSMC;
    }

    /**
     * @param value a value of {@code hmc.files.game-dir-layout}.
     * @return the layout with that name.
     * @throws IllegalArgumentException if there is no such layout.
     */
    public static GameDirLayout parse(String value) throws IllegalArgumentException {
        String name = value.trim().toLowerCase(Locale.ENGLISH).replace('_', '-');
        return Arrays.stream(values())
            .filter(layout -> layout.configName.equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown hmc.files.game-dir-layout '" + value + "', available: "
                    + Arrays.stream(values()).map(GameDirLayout::getConfigName).collect(Collectors.joining(", "))
            ));
    }

}
