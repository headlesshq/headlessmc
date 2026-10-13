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
    // "/" on Unix, but e.g. "C:\" on Windows, where "/home" would not be absolute
    private final Path root = Path.of("").toAbsolutePath().getRoot();
    private final Path home = root.resolve("home").resolve("hmc");
    private final XdgBaseDirectoriesFactory factory = new XdgBaseDirectoriesFactory();

    private Path abs(String path) {
        return root.resolve(path);
    }

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
            XdgBaseDirectories.XDG_DATA_HOME, abs("data").toString(),
            XdgBaseDirectories.XDG_CONFIG_HOME, abs("config").toString(),
            XdgBaseDirectories.XDG_STATE_HOME, abs("state").toString(),
            XdgBaseDirectories.XDG_CACHE_HOME, abs("cache").toString()
        ));

        assertEquals(abs("data"), dirs.dataHome());
        assertEquals(abs("config"), dirs.configHome());
        assertEquals(abs("state"), dirs.stateHome());
        assertEquals(abs("cache"), dirs.cacheHome());
    }

    @Test
    void fallsBackToAppDataOnWindows() {
        XdgBaseDirectories dirs = resolve(WINDOWS, Map.of(
            XdgBaseDirectories.APPDATA, abs("C/Users/hmc/AppData/Roaming").toString(),
            XdgBaseDirectories.LOCALAPPDATA, abs("C/Users/hmc/AppData/Local").toString()
        ));

        assertEquals(abs("C/Users/hmc/AppData/Roaming"), dirs.dataHome());
        assertEquals(abs("C/Users/hmc/AppData/Roaming"), dirs.configHome());
        assertEquals(abs("C/Users/hmc/AppData/Local/state"), dirs.stateHome());
        assertEquals(abs("C/Users/hmc/AppData/Local/cache"), dirs.cacheHome());
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
            XdgBaseDirectories.APPDATA, abs("C/Users/hmc/AppData/Roaming").toString(),
            XdgBaseDirectories.XDG_DATA_HOME, abs("D/data").toString(),
            XdgBaseDirectories.XDG_CACHE_HOME, abs("D/cache").toString()
        ));

        assertEquals(abs("D/data"), dirs.dataHome());
        assertEquals(abs("C/Users/hmc/AppData/Roaming"), dirs.configHome());
        assertEquals(abs("D/cache"), dirs.cacheHome());
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
