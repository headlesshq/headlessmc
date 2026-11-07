package io.github.headlesshq.headlessmc.util.json.jackson;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Files written on one operating system and read on another,
 * e.g. a HeadlessMc directory copied from a Windows machine to a Linux server.
 */
class CrossPlatformPathTest {
    public record Holder(Path file) implements ReflectionRegistered {}

    private record Source(String getIdentifier, Path getPath) implements RelativePathSource {}

    private final Logger logger = Logger.getLogger(RelativePathService.class.getName());
    private final List<LogRecord> warnings = new ArrayList<>();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                warnings.add(record);
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    };

    private FileSystem windows;
    private FileSystem unix;

    @BeforeEach
    void setUp() {
        windows = Jimfs.newFileSystem(Configuration.windows().toBuilder()
            .setRoots("C:\\", "D:\\")
            .setWorkingDirectory("C:\\work")
            .build());
        unix = Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/work").build());
        logger.addHandler(handler);
    }

    @AfterEach
    void tearDown() throws IOException {
        logger.removeHandler(handler);
        windows.close();
        unix.close();
    }

    private static DefaultJacksonJsonService service(FileSystem fs, Map<String, Path> sources) {
        return new DefaultJacksonJsonService(new RelativePathService(
            sources.entrySet().stream().map(e -> new Source(e.getKey(), e.getValue())).toList(), fs
        ));
    }

    private static String write(DefaultJacksonJsonService service, Path file) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, new Holder(file), false);
        return out.toString(StandardCharsets.UTF_8);
    }

    private static Path read(DefaultJacksonJsonService service, String json) {
        return service.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), Holder.class).file();
    }

    private static String json(String source, String sourcePath, String path) {
        return "{\"file\":{\"source\":\"" + source + "\",\"sourcePath\":\"" + sourcePath.replace("\\", "\\\\")
            + "\",\"path\":\"" + path.replace("\\", "\\\\") + "\"}}";
    }

    private String windowsMinecraftJson(String path) {
        return json("minecraft", "C:\\Users\\alice\\AppData\\Roaming\\.minecraft", path);
    }

    private Path windowsMinecraft() {
        return windows.getPath("C:\\Users\\alice\\AppData\\Roaming\\.minecraft");
    }

    private Path unixMinecraft() {
        return unix.getPath("/home/alice/.minecraft");
    }

    private static void touch(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }

    // --- Windows -> Linux ---

    @Test
    void windowsPathsAreWrittenWithForwardSlashes() {
        String json = write(service(windows, Map.of("minecraft", windowsMinecraft())),
                            windowsMinecraft().resolve("mods\\sodium.jar"));

        assertEquals(windowsMinecraftJson("mods/sodium.jar"), json);
    }

    @Test
    void windowsPathsRelativeToAKnownSourceResolveOnLinux() {
        String json = write(service(windows, Map.of("minecraft", windowsMinecraft())),
                            windowsMinecraft().resolve("versions\\1.21\\1.21.json"));

        Path path = read(service(unix, Map.of("minecraft", unixMinecraft())), json);

        assertEquals(unixMinecraft().resolve("versions/1.21/1.21.json"), path);
    }

    @Test
    void windowsRelativePathsWithoutSourceResolveOnLinux() {
        String json = write(service(windows, Map.of()), windows.getPath("mods\\sodium.jar"));

        Path path = read(service(unix, Map.of()), json);

        assertEquals(unix.getPath("mods", "sodium.jar"), path);
        assertEquals(2, path.getNameCount());
    }

    @Test
    void unknownSourceWithWindowsSourcePathFailsOnLinux() {
        // falling back to "C:\Users\..." on Linux would silently resolve to "/work/C:\Users\..."
        assertThrows(JsonParseException.class,
                     () -> read(service(unix, Map.of()), windowsMinecraftJson("mods/sodium.jar")));
    }

    @Test
    void absoluteWindowsPathsWithoutSourceAreNotAbsoluteOnLinux() {
        // known limitation: there is nothing to resolve "C:\Games\mod.jar" against on Linux,
        // it ends up as a single, relative file name.
        String json = write(service(windows, Map.of()), windows.getPath("C:\\Games\\mod.jar"));

        Path path = read(service(unix, Map.of()), json);

        assertFalse(path.isAbsolute());
        assertEquals(1, path.getNameCount());
    }

    @Test
    void windowsBackslashesInRelativePathsAreNotSeparatorsOnLinux() {
        // hand edited or legacy file: on Linux "mods\sodium.jar" is a single file name, not a directory
        Path path = read(service(unix, Map.of("minecraft", unixMinecraft())),
                         windowsMinecraftJson("mods\\sodium.jar"));

        assertEquals(unixMinecraft().resolve("mods\\sodium.jar"), path);
        assertEquals(unixMinecraft().getNameCount() + 1, path.getNameCount());
    }

    @Test
    void windowsBackslashParentSegmentsCannotEscapeOnLinux() {
        // "..\..\etc" is a single, harmless file name on Linux
        Path path = read(service(unix, Map.of("minecraft", unixMinecraft())),
                         windowsMinecraftJson("..\\..\\etc"));

        assertTrue(path.startsWith(unixMinecraft()));
    }

    @Test
    void windowsDriveLetterInPathCannotEscapeOnLinux() {
        Path path = read(service(unix, Map.of("minecraft", unixMinecraft())),
                         windowsMinecraftJson("C:\\Windows\\System32"));

        assertTrue(path.startsWith(unixMinecraft()));
    }

    // --- Linux -> Windows ---

    @Test
    void linuxPathsRelativeToAKnownSourceResolveOnWindows() {
        String json = write(service(unix, Map.of("minecraft", unixMinecraft())),
                            unixMinecraft().resolve("versions/1.21/1.21.json"));

        Path path = read(service(windows, Map.of("minecraft", windowsMinecraft())), json);

        assertEquals(windowsMinecraft().resolve("versions\\1.21\\1.21.json"), path);
    }

    @Test
    void linuxRelativePathsWithoutSourceResolveOnWindows() {
        String json = write(service(unix, Map.of()), unix.getPath("mods/sodium.jar"));

        Path path = read(service(windows, Map.of()), json);

        assertEquals(windows.getPath("mods\\sodium.jar"), path);
    }

    @Test
    void unknownSourceWithLinuxSourcePathFailsOnWindows() {
        // "/home/alice/.minecraft" is drive relative on Windows, it would resolve to "C:\home\alice\.minecraft"
        String json = json("minecraft", "/home/alice/.minecraft", "mods/sodium.jar");

        assertThrows(JsonParseException.class, () -> read(service(windows, Map.of()), json));
    }

    @Test
    void linuxBackslashParentSegmentsCannotEscapeOnWindows() {
        // "..\..\secret" is a valid file name on Linux, but a parent traversal on Windows
        String json = json("minecraft", "/home/alice/.minecraft", "..\\..\\secret");

        assertThrows(JsonParseException.class,
                     () -> read(service(windows, Map.of("minecraft", windowsMinecraft())), json));
    }

    @Test
    void driveLettersInPathCannotEscapeOnWindows() {
        for (String path : List.of("C:\\Windows\\System32", "D:\\secret", "C:secret", "\\secret", "mods/C:\\secret")) {
            String json = json("minecraft", "/home/alice/.minecraft", path);

            assertThrows(JsonParseException.class,
                         () -> read(service(windows, Map.of("minecraft", windowsMinecraft())), json), path);
        }
    }

    @Test
    void linuxFileNamesThatAreInvalidOnWindowsFailWithJsonParseException() {
        String json = json("minecraft", "/home/alice/.minecraft", "mods/what?.jar");

        assertThrows(JsonParseException.class,
                     () -> read(service(windows, Map.of("minecraft", windowsMinecraft())), json));
    }

    // --- Windows -> Windows ---

    @Test
    void movingToAnotherDriveResolvesAgainstTheNewDrive() {
        Path oldMinecraft = windowsMinecraft();
        Path newMinecraft = windows.getPath("D:\\Games\\.minecraft");
        String json = write(service(windows, Map.of("minecraft", oldMinecraft)), oldMinecraft.resolve("mods"));

        Path path = read(service(windows, Map.of("minecraft", newMinecraft)), json);

        assertEquals(newMinecraft.resolve("mods"), path);
    }

    @Test
    void pathsOnAnotherDriveThanTheSourceAreWrittenAsIs() {
        String json = write(service(windows, Map.of("minecraft", windowsMinecraft())),
                            windows.getPath("D:\\mods\\sodium.jar"));

        assertEquals("{\"file\":{\"path\":\"D:\\\\mods\\\\sodium.jar\"}}", json);
    }

    @Test
    void sourcePathsDifferingOnlyInCaseHaveNotMovedOnWindows() throws IOException {
        // Windows paths are case-insensitive, "c:/users/ALICE" is the same directory as "C:/Users/alice"
        touch(windowsMinecraft().resolve("mods\\sodium.jar"));
        String json = json("minecraft", "c:\\users\\ALICE\\appdata\\roaming\\.minecraft", "mods/sodium.jar");

        read(service(windows, Map.of("minecraft", windowsMinecraft())), json);

        assertEquals(List.of(), warnings);
    }

    // --- warnings for moved sources ---

    @Test
    void warnsIfTheFileOnlyExistsAtTheOldLocation() throws IOException {
        Path oldMinecraft = unix.getPath("/old/.minecraft");
        touch(oldMinecraft.resolve("mods/sodium.jar"));
        String json = json("minecraft", oldMinecraft.toString(), "mods/sodium.jar");

        read(service(unix, Map.of("minecraft", unixMinecraft())), json);

        assertEquals(1, warnings.size());
    }

    @Test
    void doesNotWarnIfTheFileExistsAtTheNewLocation() throws IOException {
        touch(unixMinecraft().resolve("mods/sodium.jar"));
        String json = json("minecraft", "/old/.minecraft", "mods/sodium.jar");

        read(service(unix, Map.of("minecraft", unixMinecraft())), json);

        assertEquals(List.of(), warnings);
    }

    @Test
    void doesNotWarnIfTheFileExistsAtBothLocations() throws IOException {
        touch(unix.getPath("/old/.minecraft/mods/sodium.jar"));
        touch(unixMinecraft().resolve("mods/sodium.jar"));
        String json = json("minecraft", "/old/.minecraft", "mods/sodium.jar");

        read(service(unix, Map.of("minecraft", unixMinecraft())), json);

        assertEquals(List.of(), warnings);
    }

    @Test
    void doesNotWarnIfTheFileExistsNowhere() {
        // e.g. a cache entry for a file that has not been downloaded yet
        String json = json("minecraft", "/old/.minecraft", "mods/sodium.jar");

        read(service(unix, Map.of("minecraft", unixMinecraft())), json);

        assertEquals(List.of(), warnings);
    }

    @Test
    void doesNotWarnAfterCopyingFilesFromWindowsToLinux() throws IOException {
        // the old location cannot even be represented on this machine, the file cannot exist there
        touch(unixMinecraft().resolve("mods/sodium.jar"));

        read(service(unix, Map.of("minecraft", unixMinecraft())), windowsMinecraftJson("mods/sodium.jar"));
        read(service(unix, Map.of("minecraft", unixMinecraft())), windowsMinecraftJson("mods/missing.jar"));

        assertEquals(List.of(), warnings);
    }

}
