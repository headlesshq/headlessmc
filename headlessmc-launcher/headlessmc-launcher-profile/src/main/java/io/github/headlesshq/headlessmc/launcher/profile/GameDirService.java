package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileConfig;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Decides the default game directory of a profile, the directory worlds, mods and settings are in.
 * It is decided when a profile is created, a profile keeps its game directory.
 *
 * <p>Where the game directory of a client is depends on {@code hmc.files.game}, which is a fixed directory
 * for everything, and otherwise on the {@link GameDirLayout}. With the layouts of other launchers the
 * game directory of a version is the directory the version is installed in, {@code versions/<id>}.
 * The id of a version is only known once it is installed, e.g. {@code fabric 1.21.1} is installed as
 * {@code fabric-loader-0.16.9-1.21.1}, so there are two questions:</p>
 * <ul>
 *     <li>{@link #getGameDir(VersionArg, McFiles)}: the game directory for a version, by what is known
 *     before it is installed.</li>
 *     <li>{@link #getGameDir(Path, boolean)}: the game directory for an installed version.json,
 *     if the layout places it there.</li>
 * </ul>
 * A new profile for a version that is not installed yet gets the first, as a
 * {@link #isPlaceholder(Path, VersionArg) placeholder}, and the second when its version has been installed.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class GameDirService {
    private final Holder<FileConfig> config;
    private final ConfigService configService;
    private final FileService fileService;
    private final AppFiles appFiles;

    /**
     * @return the configured layout of game directories.
     * @throws IllegalArgumentException if {@code hmc.files.game-dir-layout} is not the name of a layout.
     */
    public GameDirLayout getLayout() throws IllegalArgumentException {
        return GameDirLayout.parse(config.get().gameDirLayout());
    }

    /**
     * The default game directory for a version.
     * For clients this is a directory in a {@code .minecraft} directory, unless a game directory has been configured.
     * It only depends on the configuration and the arguments, it is decided when a profile is created.
     *
     * <p>With a layout that places game directories {@link GameDirLayout#isInVersionDirectory() in the directory
     * of the installed version}, this is the directory a version installed under the name of the given version
     * has, e.g. {@code versions/fabric-1.21.1}. That is exact for vanilla versions and otherwise a
     * {@link #isPlaceholder(Path, VersionArg) placeholder}: once the version.json is known,
     * {@link #getGameDir(Path, boolean)} has the answer.</p>
     *
     * @param version the version to get the game directory for.
     * @param mcFiles the {@code .minecraft} directory to place the game directory of a client in,
     *                the directory the version is, or is going to be, installed in.
     * @return the default game directory for the version.
     */
    public Path getGameDir(VersionArg version, McFiles mcFiles) {
        if (Side.SERVER.equals(version.side().orElse(Side.CLIENT))) {
            // TODO: not super happy with this, should be placed deeper,
            //  make a distinction between default profiles and explicit profiles
            return appFiles.getServerDir().resolve("server-" + version.toString("-"));
        }

        Optional<String> gameDir = getConfiguredGameDir();
        if (gameDir.isPresent()) {
            return fileService.getUserPath(gameDir.get());
        }

        GameDirLayout layout = getLayout();
        if (layout == GameDirLayout.VERSIONS_MODDED && version.isVanilla()) {
            return mcFiles.getMcDir();
        }

        if (layout.isInVersionDirectory()) {
            return mcFiles.getVersionsDir().resolve(getPlaceholderName(version));
        }

        if (config.get().gameForEachVersion()) {
            return mcFiles.getMcDir().resolve(version.toString("-"));
        }

        return mcFiles.getMcDir();
    }

    /**
     * The default game directory for an installed version, if it depends on where the version is installed.
     * That is the case for layouts that place game directories
     * {@link GameDirLayout#isInVersionDirectory() in the directory of the installed version},
     * and a version.json that is installed like launchers install versions: {@code <dir>/versions/<id>/<id>.json}.
     *
     * @param versionJson the version.json of the installed version.
     * @param modded      whether the version is a modded version. If that cannot be told it is one:
     *                    a version that is not known to be vanilla keeps its files to itself.
     * @return the game directory for the version, or none if it does not depend on the version.json,
     * then {@link #getGameDir(VersionArg, McFiles)} is the game directory.
     */
    public Optional<Path> getGameDir(Path versionJson, boolean modded) {
        if (getConfiguredGameDir().isPresent()) {
            return Optional.empty();
        }

        GameDirLayout layout = getLayout();
        Path file = versionJson.toAbsolutePath().normalize();
        @Nullable Path versionDir = file.getParent();
        @Nullable Path versionsDir = versionDir == null ? null : versionDir.getParent();
        @Nullable Path mcDir = versionsDir == null ? null : versionsDir.getParent();
        if (!layout.isInVersionDirectory() || mcDir == null || !McFiles.of(mcDir).getVersionsDir().equals(versionsDir)) {
            return Optional.empty();
        }

        return Optional.of(layout == GameDirLayout.VERSIONS_MODDED && !modded ? mcDir : versionDir);
    }

    /**
     * Checks if a game directory is the directory {@link #getGameDir(VersionArg, McFiles)} returns for a
     * version that is not installed yet: {@code versions/<name of the version>} in some {@code .minecraft}
     * directory, with a layout that places game directories in the directory of the installed version.
     * A new profile has this game directory until its version has been installed, it stands for
     * "the directory of the version". Nothing but a profile created with such a layout has it.
     *
     * @param gameDir the game directory of a profile.
     * @param version the version the profile has been created for.
     * @return {@code true} if the game directory is to be replaced by {@link #getGameDir(Path, boolean)}
     * once the version of the profile is installed.
     */
    public boolean isPlaceholder(Path gameDir, VersionArg version) {
        if (getConfiguredGameDir().isPresent() || !getLayout().isInVersionDirectory()) {
            return false;
        }

        Path path = gameDir.toAbsolutePath().normalize();
        @Nullable Path versionsDir = path.getParent();
        @Nullable Path mcDir = versionsDir == null ? null : versionsDir.getParent();
        return mcDir != null
            && path.equals(McFiles.of(mcDir).getVersionsDir().resolve(getPlaceholderName(version)));
    }

    private static String getPlaceholderName(VersionArg version) {
        return version.withSide(null).toString("-");
    }

    private Optional<String> getConfiguredGameDir() {
        FileConfig config = this.config.get();
        Optional<String> legacyGameDir = configService.getConfig().getOptionalValue("hmc.gamedir", String.class);
        return config.property(
            config.gameDir(),
            legacyGameDir,
            log::warn,
            "hmc.gamedir",
            "hmc.files.game"
        );
    }

}
