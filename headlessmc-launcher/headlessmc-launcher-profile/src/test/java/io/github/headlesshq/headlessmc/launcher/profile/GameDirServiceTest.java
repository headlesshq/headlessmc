package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GameDirServiceTest {
    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final Path root = fileService.getUserPath("/root");
    private final McFiles mcFiles = McFiles.of(root.resolve("mc"));
    private final McFiles ssd = McFiles.of(root.resolve("ssd").resolve(".minecraft"));
    private final AppFiles appFiles = TestFiles.appFiles(root);
    private final TestGameDirService service = new TestGameDirService(appFiles);

    private VersionArg version(Side side) {
        return new VersionArg(Optional.of(side), "vanilla", "1.21.1", Optional.empty());
    }

    private static Path json(McFiles mcFiles, String id) {
        return mcFiles.getVersionsDir().resolve(id).resolve(id + ".json");
    }

    @Test
    void serverVersionsGetAPerVersionServerDir() {
        VersionArg version = version(Side.SERVER);

        assertEquals(
            appFiles.getServerDir().resolve("server-" + version.toString("-")),
            service.getGameDir(version, mcFiles)
        );
    }

    @Test
    void clientVersionsGetAPerVersionMcDirWhenEnabled() {
        VersionArg version = version(Side.CLIENT);

        assertEquals(mcFiles.getMcDir().resolve(version.toString("-")), service.getGameDir(version, mcFiles));
    }

    @Test
    void clientVersionsShareTheMcDirWhenPerVersionIsDisabled() {
        service.withGameForEachVersion(false);

        assertEquals(mcFiles.getMcDir(), service.getGameDir(version(Side.CLIENT), mcFiles));
    }

    @Test
    void usesTheConfiguredGameDir() {
        service.withGameDir("/configured/game");

        assertEquals(fileService.getUserPath("/configured/game"), service.getGameDir(version(Side.CLIENT), mcFiles));
    }

    @Test
    void fallsBackToTheLegacyGameDirProperty() {
        service.withLegacyGameDir("/legacy/game");

        assertEquals(fileService.getUserPath("/legacy/game"), service.getGameDir(version(Side.CLIENT), mcFiles));
    }

    @Test
    void conflictingGameDirAndLegacyGameDirThrow() {
        service.withGameDir("/a").withLegacyGameDir("/b");

        assertThrows(ConfigException.class, () -> service.getGameDir(version(Side.CLIENT), mcFiles));
    }

    // the layout of game directories

    @Test
    void theDefaultLayoutIsTheOneOfHeadlessMc() {
        assertEquals(GameDirLayout.HEADLESSMC, service.getLayout());
        assertFalse(service.getLayout().isInVersionDirectory());
        // nothing depends on where a version is installed
        assertEquals(Optional.empty(), service.getGameDir(json(mcFiles, "fabric-loader-0.16.9-1.21.1"), true));
        assertFalse(service.isPlaceholder(mcFiles.getMcDir().resolve("fabric-1.21.1"), VersionArg.parse("fabric", "1.21.1")));
        assertFalse(service.isPlaceholder(mcFiles.getVersionsDir().resolve("fabric-1.21.1"), VersionArg.parse("fabric", "1.21.1")));
    }

    @Test
    void layoutsAreParsedFromTheirConfigNames() {
        assertEquals(GameDirLayout.VERSIONS, service.withLayout("versions").getLayout());
        assertEquals(GameDirLayout.VERSIONS_MODDED, service.withLayout("versions-modded").getLayout());
        assertEquals(GameDirLayout.VERSIONS_MODDED, service.withLayout(" Versions_Modded ").getLayout());
        assertEquals(GameDirLayout.HEADLESSMC, service.withLayout("headlessmc").getLayout());
        for (GameDirLayout layout : GameDirLayout.values()) {
            assertEquals(layout, GameDirLayout.parse(layout.getConfigName()));
        }
    }

    @Test
    void anUnknownLayoutIsAnErrorThatNamesTheLayouts() {
        service.withLayout("hmcl");

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.getGameDir(version(Side.CLIENT), mcFiles)
        );

        assertTrue(e.getMessage().contains("hmc.files.game-dir-layout 'hmcl'"), e.getMessage());
        assertTrue(e.getMessage().contains("headlessmc, versions, versions-modded"), e.getMessage());
    }

    @Test
    void inTheVersionsLayoutTheGameDirectoryIsTheDirectoryOfTheInstalledVersion() {
        service.withLayout(GameDirLayout.VERSIONS);
        Path fabric = json(mcFiles, "fabric-loader-0.16.9-1.21.1");
        Path vanilla = json(ssd, "1.21.1");

        assertEquals(Optional.of(fabric.getParent()), service.getGameDir(fabric, true));
        // also vanilla versions, and in whatever .minecraft directory the version is installed
        assertEquals(Optional.of(ssd.getVersionsDir().resolve("1.21.1")), service.getGameDir(vanilla, false));
        // the name of a version.json from another launcher is whatever the user called it
        assertEquals(Optional.of(mcFiles.getVersionsDir().resolve("My Pack")), service.getGameDir(json(mcFiles, "My Pack"), true));
    }

    @Test
    void inTheVersionsLayoutAVersionThatIsNotInstalledGetsAPlaceholder() {
        service.withLayout(GameDirLayout.VERSIONS);
        VersionArg fabric = VersionArg.parse("fabric", "1.21.1");

        Path placeholder = service.getGameDir(fabric, ssd);

        // the id it will be installed under is not known, e.g. fabric-loader-0.16.9-1.21.1
        assertEquals(ssd.getVersionsDir().resolve("fabric-1.21.1"), placeholder);
        assertTrue(service.isPlaceholder(placeholder, fabric));
        assertTrue(service.isPlaceholder(placeholder, fabric.withSide(Side.CLIENT)));
        // for a vanilla version it is the directory it will be installed in
        assertEquals(mcFiles.getVersionsDir().resolve("1.21.1"), service.getGameDir(VersionArg.parse("1.21.1"), mcFiles));
        // game directories that are something else
        assertFalse(service.isPlaceholder(ssd.getVersionsDir().resolve("fabric-loader-0.16.9-1.21.1"), fabric));
        assertFalse(service.isPlaceholder(ssd.getMcDir().resolve("fabric-1.21.1"), fabric));
        assertFalse(service.isPlaceholder(ssd.getMcDir(), fabric));
        assertFalse(service.isPlaceholder(root.resolve("games").resolve("fabric-1.21.1"), fabric));
        assertFalse(service.isPlaceholder(placeholder, VersionArg.parse("forge", "1.21.1")));
        assertFalse(service.isPlaceholder(root.getRoot(), fabric));
    }

    @Test
    void inTheVersionsModdedLayoutVanillaVersionsShareTheMinecraftDirectory() {
        service.withLayout(GameDirLayout.VERSIONS_MODDED);

        // like HMCL does by default
        assertEquals(Optional.of(mcFiles.getMcDir()), service.getGameDir(json(mcFiles, "1.21.1"), false));
        assertEquals(mcFiles.getMcDir(), service.getGameDir(VersionArg.parse("1.21.1"), mcFiles));
        assertEquals(
            Optional.of(mcFiles.getVersionsDir().resolve("fabric-loader-0.16.9-1.21.1")),
            service.getGameDir(json(mcFiles, "fabric-loader-0.16.9-1.21.1"), true)
        );
        assertEquals(
            mcFiles.getVersionsDir().resolve("fabric-1.21.1"),
            service.getGameDir(VersionArg.parse("fabric", "1.21.1"), mcFiles)
        );
        assertTrue(service.isPlaceholder(mcFiles.getVersionsDir().resolve("fabric-1.21.1"), VersionArg.parse("fabric", "1.21.1")));
    }

    @Test
    void aVersionJsonThatIsNotInstalledLikeAVersionHasNoDirectoryOfItsOwn() {
        service.withLayout(GameDirLayout.VERSIONS);

        // a loose file, e.g. in the downloads directory: its directory is not a game directory
        assertEquals(Optional.empty(), service.getGameDir(root.resolve("downloads").resolve("pack.json"), true));
        assertEquals(Optional.empty(), service.getGameDir(root.resolve("pack.json"), true));
        assertEquals(Optional.empty(), service.getGameDir(mcFiles.getMcDir().resolve("other").resolve("pack").resolve("pack.json"), true));
    }

    @Test
    void aConfiguredGameDirectoryWinsOverEveryLayout() {
        service.withLayout(GameDirLayout.VERSIONS).withGameDir("/configured/game");
        VersionArg fabric = VersionArg.parse("fabric", "1.21.1");

        assertEquals(fileService.getUserPath("/configured/game"), service.getGameDir(fabric, mcFiles));
        assertEquals(Optional.empty(), service.getGameDir(json(mcFiles, "fabric-loader-0.16.9-1.21.1"), true));
        assertFalse(service.isPlaceholder(mcFiles.getVersionsDir().resolve("fabric-1.21.1"), fabric));
    }

    @Test
    void serversAreNotAffectedByTheLayout() {
        service.withLayout(GameDirLayout.VERSIONS);
        VersionArg version = version(Side.SERVER);

        assertEquals(appFiles.getServerDir().resolve("server-" + version.toString("-")), service.getGameDir(version, mcFiles));
    }

    @Test
    void gameForEachVersionIsOnlyUsedByTheHeadlessMcLayout() {
        service.withLayout(GameDirLayout.VERSIONS).withGameForEachVersion(false);

        assertEquals(mcFiles.getVersionsDir().resolve("1.21.1"), service.getGameDir(VersionArg.parse("1.21.1"), mcFiles));
    }

}
