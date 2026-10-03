package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.os.OS;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XdgBaseDirectoriesFactoryTest {
    private static final OS LINUX = new OS("linux", OS.Type.LINUX, "6.17");
    private static final OS WINDOWS = new OS("windows", OS.Type.WINDOWS, "11");
    private static final OS MACOS = new OS("macos", OS.Type.MACOS, "15");

    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final Path home = Path.of("/home", "hmc");
    private final XdgBaseDirectoriesFactory factory = new XdgBaseDirectoriesFactory();

    private XdgBaseDirectories resolve(OS os, Map<String, @Nullable String> environment) {
        return factory.resolve(fileService, os, environment::get, home);
    }

    @Test
    void usesTheXdgDefaultsIfNoVariablesAreSet() {
        XdgBaseDirectories dirs = resolve(LINUX, Map.of());

        assertEquals(home.resolve(".local/share"), dirs.dataHome());
        assertEquals(home.resolve(".config"), dirs.configHome());
        assertEquals(home.resolve(".local/state"), dirs.stateHome());
        assertEquals(home.resolve(".cache"), dirs.cacheHome());
    }

    @Test
    void usesTheXdgVariablesIfTheyAreSet() {
        XdgBaseDirectories dirs = resolve(LINUX, Map.of(
            XdgBaseDirectories.XDG_DATA_HOME, "/data",
            XdgBaseDirectories.XDG_CONFIG_HOME, "/config",
            XdgBaseDirectories.XDG_STATE_HOME, "/state",
            XdgBaseDirectories.XDG_CACHE_HOME, "/cache"
        ));

        assertEquals(Path.of("/data"), dirs.dataHome());
        assertEquals(Path.of("/config"), dirs.configHome());
        assertEquals(Path.of("/state"), dirs.stateHome());
        assertEquals(Path.of("/cache"), dirs.cacheHome());
    }

    @Test
    void fallsBackToAppDataOnWindows() {
        XdgBaseDirectories dirs = resolve(WINDOWS, Map.of(
            XdgBaseDirectories.APPDATA, "/C/Users/hmc/AppData/Roaming",
            XdgBaseDirectories.LOCALAPPDATA, "/C/Users/hmc/AppData/Local"
        ));

        assertEquals(Path.of("/C/Users/hmc/AppData/Roaming"), dirs.dataHome());
        assertEquals(Path.of("/C/Users/hmc/AppData/Roaming"), dirs.configHome());
        assertEquals(Path.of("/C/Users/hmc/AppData/Local/state"), dirs.stateHome());
        assertEquals(Path.of("/C/Users/hmc/AppData/Local/cache"), dirs.cacheHome());
    }

    @Test
    void fallsBackToTheUserHomeIfWindowsHasNoAppData() {
        XdgBaseDirectories dirs = resolve(WINDOWS, Map.of());

        assertEquals(home.resolve("AppData/Roaming"), dirs.dataHome());
        assertEquals(home.resolve("AppData/Local/state"), dirs.stateHome());
        assertEquals(home.resolve("AppData/Local/cache"), dirs.cacheHome());
    }

    @Test
    void theXdgVariablesWinOnWindowsToo() {
        XdgBaseDirectories dirs = resolve(WINDOWS, Map.of(
            XdgBaseDirectories.APPDATA, "/C/Users/hmc/AppData/Roaming",
            XdgBaseDirectories.XDG_DATA_HOME, "/D/data",
            XdgBaseDirectories.XDG_CACHE_HOME, "/D/cache"
        ));

        assertEquals(Path.of("/D/data"), dirs.dataHome());
        assertEquals(Path.of("/C/Users/hmc/AppData/Roaming"), dirs.configHome());
        assertEquals(Path.of("/D/cache"), dirs.cacheHome());
    }

    @Test
    void usesTheLibraryDirectoriesOnMacOs() {
        XdgBaseDirectories dirs = resolve(MACOS, Map.of());

        assertEquals(home.resolve("Library/Application Support"), dirs.dataHome());
        assertEquals(home.resolve("Library/Application Support"), dirs.configHome());
        assertEquals(home.resolve("Library/Application Support"), dirs.stateHome());
        assertEquals(home.resolve("Library/Caches"), dirs.cacheHome());
    }

    @Test
    void ignoresRelativeAndEmptyVariables() {
        XdgBaseDirectories dirs = resolve(LINUX, Map.of(
            XdgBaseDirectories.XDG_DATA_HOME, "relative/data",
            XdgBaseDirectories.XDG_CACHE_HOME, "  "
        ));

        assertEquals(home.resolve(".local/share"), dirs.dataHome());
        assertEquals(home.resolve(".cache"), dirs.cacheHome());
    }

    @Test
    void ignoresAnInvalidPath() {
        XdgBaseDirectories dirs = resolve(LINUX, Map.of(XdgBaseDirectories.XDG_DATA_HOME, "invalid" + '\0' + "path"));

        assertEquals(home.resolve(".local/share"), dirs.dataHome());
    }

    @Test
    void thePublicEntryPointReadsTheRealEnvironmentAndUserHome() {
        XdgBaseDirectories dirs = factory.resolve(fileService, LINUX);

        assertTrue(dirs.dataHome().isAbsolute());
        assertTrue(dirs.configHome().isAbsolute());
        assertTrue(dirs.stateHome().isAbsolute());
        assertTrue(dirs.cacheHome().isAbsolute());
    }

}
