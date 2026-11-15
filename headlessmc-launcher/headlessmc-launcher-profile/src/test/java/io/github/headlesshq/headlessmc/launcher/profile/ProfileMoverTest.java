package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProfileMoverTest {
    @TempDir
    Path root;

    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private FakeProfileService profileService;
    private TestLaunchTargets targets;
    private ProfileLaunchTarget profileTarget;
    private FakeLaunchTarget base;
    private RelocatingTarget relocating;
    private TestEvent<ProfileMoveEvent> moveEvent;
    private TestEvent<ProfileMovedEvent> movedEvent;
    private McFiles mcFiles;
    private ProfileMover mover;

    /** A target that remembers a file, like a server remembers its jar. */
    private static final class RelocatingTarget extends FakeLaunchTarget {
        private RelocatingTarget() {
            super("relocating");
        }

        @Override
        public LaunchTargetHandle relocate(Profile profile, Path newPath) {
            Path file = Path.of(value(profile.target()));
            return file.startsWith(profile.path())
                ? handle(newPath.resolve(profile.path().relativize(file).toString()).toString())
                : profile.target();
        }
    }

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root);
        targets = new TestLaunchTargets(profileService);
        profileTarget = targets.profileTarget();
        base = new FakeLaunchTarget("base").withSide(Side.CLIENT);
        relocating = new RelocatingTarget();
        targets.with(profileTarget, base, relocating);
        moveEvent = new TestEvent<>();
        movedEvent = new TestEvent<>();
        mcFiles = McFiles.of(root.resolve("mc"));
        mover = new ProfileMover(
            targets.service(), profileTarget, profileService, McFilesService.of(mcFiles), fileService,
            moveEvent, movedEvent
        );
    }

    private Profile profile(String name) {
        Profile profile = new Profile(name, Optional.of(Side.CLIENT), base.handle("x"), root.resolve("games").resolve(name));
        return profileService.save(profile);
    }

    private Profile withFiles(Profile profile) {
        write(profile.path().resolve("options.txt"), "options of " + profile.name());
        write(profile.path().resolve("saves").resolve("world").resolve("level.dat"), "level");
        return profile;
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ProfileMover.Result move(Profile profile, Path newPath, String newName) {
        return mover.move(new ProfileMover.Request(profile, newPath, newName, false, false));
    }

    // moving

    @Test
    void movesTheGameDirectoryAndUpdatesTheProfile() {
        Profile profile = withFiles(profile("main"));
        Path newPath = root.resolve("elsewhere").resolve("main");

        ProfileMover.Result result = move(profile, newPath, null);

        assertTrue(result.filesMoved());
        assertEquals(profile, result.before());
        // only the path of the profile changed
        assertEquals(profile.withPath(newPath), result.after());
        assertEquals(Optional.of(result.after()), profileService.getProfile("main"));
        assertEquals("options of main", read(newPath.resolve("options.txt")));
        assertEquals("level", read(newPath.resolve("saves").resolve("world").resolve("level.dat")));
        assertFalse(Files.exists(profile.path()));
        assertEquals(List.of(), result.updated());
    }

    @Test
    void movesIntoAnExistingEmptyDirectory() throws IOException {
        Profile profile = withFiles(profile("main"));
        Path newPath = Files.createDirectories(root.resolve("empty"));

        move(profile, newPath, null);

        assertEquals("options of main", read(newPath.resolve("options.txt")));
    }

    @Test
    void aProfileThatHasNeverBeenLaunchedOnlyGetsANewPath() {
        Profile profile = profile("main");
        Path newPath = root.resolve("elsewhere");

        ProfileMover.Result result = move(profile, newPath, null);

        // there is nothing to move yet
        assertFalse(result.filesMoved());
        assertEquals(newPath, profileService.getProfile("main").orElseThrow().path());
        assertFalse(Files.exists(newPath));
    }

    @Test
    void copyingADirectoryCopiesEverything() throws IOException {
        // used when a directory cannot be renamed, e.g. because the new directory is on another disk
        Profile profile = withFiles(profile("main"));
        Files.createDirectories(profile.path().resolve("empty-dir"));
        Path destination = root.resolve("copy");

        mover.copyDirectory(profile.path(), destination);

        assertEquals("options of main", read(destination.resolve("options.txt")));
        assertEquals("level", read(destination.resolve("saves").resolve("world").resolve("level.dat")));
        assertTrue(Files.isDirectory(destination.resolve("empty-dir")));
        // the source is only deleted by the caller once everything has been copied
        assertTrue(Files.exists(profile.path().resolve("options.txt")));
        try (Stream<Path> source = Files.walk(profile.path()); Stream<Path> copy = Files.walk(destination)) {
            assertEquals(source.count(), copy.count());
        }
    }

    // renaming

    @Test
    void renamesAProfileWithoutTouchingItsFiles() {
        Profile profile = withFiles(profile("main"));

        ProfileMover.Result result = move(profile, null, "survival");

        assertFalse(result.filesMoved());
        assertEquals(profile.withName("survival"), result.after());
        assertEquals(Optional.empty(), profileService.getProfile("main"));
        assertEquals(Optional.of(result.after()), profileService.getProfile("survival"));
        assertEquals("options of main", read(profile.path().resolve("options.txt")));
    }

    @Test
    void movesAndRenamesAtOnce() {
        Profile profile = withFiles(profile("main"));
        Path newPath = root.resolve("survival");

        ProfileMover.Result result = move(profile, newPath, "survival");

        assertEquals(profile.withName("survival").withPath(newPath), result.after());
        assertEquals(1, profileService.getProfiles().size());
        assertEquals("options of main", read(newPath.resolve("options.txt")));
    }

    @Test
    void changingOnlyTheCaseOfANameKeepsTheProfile() {
        Profile profile = profile("main");

        ProfileMover.Result result = move(profile, null, "Main");

        assertEquals("Main", result.after().name());
        assertEquals(List.of("Main"), profileService.getProfiles().stream().map(Profile::name).toList());
    }

    @Test
    void renamingAParentUpdatesItsChildren() {
        Profile parent = profile("main");
        Profile child = profileService.save(targets.service().create(
            "debug", targets.service().parse(ProfileLaunchTarget.NAME, TargetArgs.of("main")), new TypedMapImpl()
        ).withPath(root.resolve("games").resolve("debug")));
        Profile unrelated = profile("other");

        ProfileMover.Result result = move(parent, null, "survival");

        Profile updatedChild = profileService.getProfile("debug").orElseThrow();
        assertEquals(Optional.of("survival"), profileTarget.getParentName(updatedChild));
        assertEquals(List.of(updatedChild), result.updated());
        // and it can still be launched
        assertEquals("base", targets.service().effective(updatedChild).target().name());
        assertEquals(Optional.of(unrelated), profileService.getProfile("other"));
        assertEquals(child.withTarget(updatedChild.target()), updatedChild);
    }

    // launch targets remembering files

    @Test
    void filesATargetRemembersInsideTheGameDirectoryFollowTheMove() {
        Path oldPath = root.resolve("games").resolve("srv");
        Profile profile = profileService.save(new Profile(
            "srv", Optional.empty(), relocating.handle(oldPath.resolve("server.jar").toString()), oldPath
        ));
        write(oldPath.resolve("server.jar"), "jar");
        Path newPath = root.resolve("servers").resolve("srv");

        ProfileMover.Result result = move(profile, newPath, null);

        assertEquals(newPath.resolve("server.jar").toString(), FakeLaunchTarget.value(result.after().target()));
        assertEquals("jar", read(newPath.resolve("server.jar")));
    }

    @Test
    void filesOutsideTheGameDirectoryAreNotTouched() {
        Path outside = root.resolve("shared").resolve("tool.jar");
        Profile profile = profileService.save(new Profile(
            "tool", Optional.empty(), relocating.handle(outside.toString()), root.resolve("games").resolve("tool")
        ));

        ProfileMover.Result result = move(profile, root.resolve("elsewhere"), null);

        assertEquals(outside.toString(), FakeLaunchTarget.value(result.after().target()));
    }

    @Test
    void aProfileWithAnUnknownTargetCanStillBeMoved() {
        Profile profile = withFiles(profileService.save(new Profile(
            "plugin", Optional.of(Side.CLIENT), new LaunchTargetHandle("from-a-plugin", "x"), root.resolve("plugin")
        )));

        ProfileMover.Result result = move(profile, root.resolve("moved"), null);

        assertEquals(profile.target(), result.after().target());
        assertEquals("options of plugin", read(root.resolve("moved").resolve("options.txt")));
    }

    // shared game directories

    @Test
    void aGameDirectoryUsedByOtherProfilesIsNotMovedByDefault() {
        Profile profile = withFiles(profile("main"));
        Profile sharing = profileService.save(profile.withName("debug"));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> move(profile, root.resolve("elsewhere"), null)
        );

        assertTrue(e.getMessage().contains("also used by debug"), e.getMessage());
        assertTrue(e.getMessage().contains("--with-shared"), e.getMessage());
        assertNothingChanged(profile);
        assertEquals(Optional.of(sharing), profileService.getProfile("debug"));
        assertEquals(List.of(sharing), mover.getProfilesSharing(profile));
    }

    @Test
    void withSharedTheDirectoryIsMovedOnceForAllProfiles() {
        Profile profile = withFiles(profile("main"));
        profileService.save(profile.withName("debug"));
        Path newPath = root.resolve("elsewhere");

        ProfileMover.Result result = mover.move(new ProfileMover.Request(profile, newPath, null, true, false));

        assertEquals(newPath, profileService.getProfile("main").orElseThrow().path());
        assertEquals(newPath, profileService.getProfile("debug").orElseThrow().path());
        assertEquals(List.of("debug"), result.updated().stream().map(Profile::name).toList());
        assertEquals("options of main", read(newPath.resolve("options.txt")));
        // observers are asked for every profile that is moved
        assertEquals(List.of("main", "debug"), moveEvent.fired().stream().map(event -> event.profile().name()).toList());
    }

    @Test
    void aChildSharingTheDirectoryOfARenamedParentIsUpdatedOnce() {
        Profile parent = withFiles(profile("main"));
        // a child plays in the game directory of its parent by default
        Profile child = profileService.save(targets.service().create(
            "debug", targets.service().parse(ProfileLaunchTarget.NAME, TargetArgs.of("main")), new TypedMapImpl()
        ));
        assertEquals(parent.path(), child.path());
        Path newPath = root.resolve("elsewhere");

        ProfileMover.Result result = mover.move(new ProfileMover.Request(parent, newPath, "survival", true, false));

        Profile updated = profileService.getProfile("debug").orElseThrow();
        assertEquals(newPath, updated.path());
        assertEquals(Optional.of("survival"), profileTarget.getParentName(updated));
        assertEquals(List.of(updated), result.updated());
    }

    // refusals, nothing is changed by any of them

    private void assertNothingChanged(Profile profile) {
        assertEquals(Optional.of(profile), profileService.getProfile(profile.name()));
        assertEquals("options of " + profile.name(), read(profile.path().resolve("options.txt")));
        assertEquals(List.of(), movedEvent.fired());
    }

    @Test
    void nothingToDoIsAnError() {
        Profile profile = withFiles(profile("main"));

        assertThrows(IllegalArgumentException.class, () -> move(profile, null, null));
        assertThrows(IllegalArgumentException.class, () -> move(profile, profile.path(), "main"));
        assertNothingChanged(profile);
        assertEquals(List.of(), moveEvent.fired());
    }

    @Test
    void aNameThatIsTakenIsNotOverwritten() {
        Profile profile = withFiles(profile("main"));
        Profile other = profile("other");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> move(profile, null, "OTHER"));

        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
        assertNothingChanged(profile);
        assertEquals(Optional.of(other), profileService.getProfile("other"));
    }

    @Test
    void forceOverwritesAProfileWithTheNewName() {
        Profile profile = profile("main");
        profile("other");

        mover.move(new ProfileMover.Request(profile, null, "other", false, true));

        assertEquals(List.of("other"), profileService.getProfiles().stream().map(Profile::name).toList());
        assertEquals(profile.path(), profileService.getProfile("other").orElseThrow().path());
    }

    @Test
    void anEmptyNameIsRejected() {
        Profile profile = withFiles(profile("main"));

        assertThrows(IllegalArgumentException.class, () -> move(profile, null, " "));
        assertNothingChanged(profile);
    }

    @Test
    void aDestinationThatIsNotEmptyIsRejected() {
        Profile profile = withFiles(profile("main"));
        Path newPath = root.resolve("occupied");
        write(newPath.resolve("file.txt"), "important");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> move(profile, newPath, null));

        assertTrue(e.getMessage().contains("not empty"), e.getMessage());
        assertNothingChanged(profile);
        assertEquals("important", read(newPath.resolve("file.txt")));
    }

    @Test
    void aDestinationThatIsAFileIsRejected() {
        Profile profile = withFiles(profile("main"));
        Path newPath = root.resolve("file");
        write(newPath, "a file");

        assertThrows(IllegalArgumentException.class, () -> move(profile, newPath, null));
        assertNothingChanged(profile);
    }

    @Test
    void aDirectoryCannotBeMovedIntoItselfOrItsParent() {
        Profile profile = withFiles(profile("main"));

        assertThrows(IllegalArgumentException.class, () -> move(profile, profile.path().resolve("inner"), null));
        assertThrows(IllegalArgumentException.class, () -> move(profile, profile.path().getParent(), null));
        assertNothingChanged(profile);
    }

    @Test
    void aGameDirectoryThatIsTheMinecraftDirectoryCannotBeMoved() {
        // hmc.files.game-for-each-version=false makes the .minecraft directory the game directory
        Profile profile = profileService.save(new Profile(
            "main", Optional.of(Side.CLIENT), base.handle("x"), mcFiles.getMcDir()
        ));
        write(mcFiles.getMcDir().resolve("options.txt"), "options of main");

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> move(profile, root.resolve("elsewhere"), null)
        );

        assertTrue(e.getMessage().contains("is the Minecraft directory"), e.getMessage());
        assertNothingChanged(profile);
        // renaming such a profile is fine
        assertEquals("renamed", move(profile, null, "renamed").after().name());
    }

    @Test
    void aGameDirectoryContainingTheMinecraftDirectoryCannotBeMoved() {
        Profile profile = profileService.save(new Profile(
            "main", Optional.of(Side.CLIENT), base.handle("x"), root
        ));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> move(profile, root.resolveSibling("elsewhere"), null)
        );

        assertTrue(e.getMessage().contains("contains the Minecraft directory"), e.getMessage());
    }

    // events

    @Test
    void anObserverCanPreventTheMove() {
        Profile profile = withFiles(profile("main"));
        moveEvent.observe(event -> {
            throw new IllegalStateException(event.profile().name() + " is running");
        });

        IllegalStateException e = assertThrows(
            IllegalStateException.class, () -> move(profile, root.resolve("elsewhere"), "renamed")
        );

        assertEquals("main is running", e.getMessage());
        assertNothingChanged(profile);
        assertFalse(Files.exists(root.resolve("elsewhere")));
    }

    @Test
    void theEventsDescribeTheMove() {
        Profile profile = withFiles(profile("main"));
        Path newPath = root.resolve("elsewhere");

        ProfileMover.Result result = move(profile, newPath, "renamed");

        ProfileMoveEvent before = moveEvent.fired().getFirst();
        assertEquals(new ProfileMoveEvent(profile, "renamed", newPath.toAbsolutePath().normalize()), before);
        assertTrue(before.isMove());
        assertTrue(before.isRename());
        assertEquals(List.of(new ProfileMovedEvent(profile, result.after())), movedEvent.fired());
        assertTrue(movedEvent.fired().getFirst().isRename());
    }

    @Test
    void aRenameIsNotAMove() {
        Profile profile = profile("main");

        move(profile, null, "renamed");

        assertFalse(moveEvent.fired().getFirst().isMove());
        assertTrue(moveEvent.fired().getFirst().isRename());
    }

    @Test
    void theEventIsOnlyFiredForAValidMove() {
        Profile profile = withFiles(profile("main"));
        profile("other");

        assertThrows(IllegalArgumentException.class, () -> move(profile, null, "other"));

        assertEquals(List.of(), moveEvent.fired());
    }

    @Test
    void anObserverFailingAfterTheMoveDoesNotUndoIt() {
        Profile profile = withFiles(profile("main"));
        movedEvent.observe(event -> {
            throw new IllegalStateException("backups could not follow");
        });

        ProfileMover.Result result = move(profile, root.resolve("elsewhere"), null);

        assertEquals(Optional.of(result.after()), profileService.getProfile("main"));
        assertEquals("options of main", read(root.resolve("elsewhere").resolve("options.txt")));
    }

    // game directories that are the directory of an installed version, see GameDirLayout

    /** A profile playing in the directory of the version {@code id}, like HMCL and PCL2 do. */
    private Profile inVersionDirectory(String name, String id) {
        Path versionDir = mcFiles.getVersionsDir().resolve(id);
        write(versionDir.resolve(id + ".json"), "json");
        write(versionDir.resolve(id + ".jar"), "jar");
        write(versionDir.resolve("natives-linux").resolve("lwjgl.so"), "natives");
        return profileService.save(new Profile(name, Optional.of(Side.CLIENT), base.handle("x"), versionDir));
    }

    @Test
    void movingAGameOutOfAVersionDirectoryLeavesTheVersionInstalled() {
        Profile profile = withFiles(inVersionDirectory("main", "fabric-loader-0.16.9-1.21.1"));
        Path newPath = root.resolve("elsewhere").resolve("main");

        ProfileMover.Result result = move(profile, newPath, null);

        assertTrue(result.filesMoved());
        assertEquals(newPath, profileService.getProfile("main").orElseThrow().path());
        // the game is at the new path
        assertEquals("options of main", read(newPath.resolve("options.txt")));
        assertEquals("level", read(newPath.resolve("saves").resolve("world").resolve("level.dat")));
        assertFalse(Files.exists(profile.path().resolve("options.txt")));
        assertFalse(Files.exists(profile.path().resolve("saves")));
        // the version is still installed, other profiles and launchers can still launch it
        assertEquals("json", read(profile.path().resolve("fabric-loader-0.16.9-1.21.1.json")));
        assertEquals("jar", read(profile.path().resolve("fabric-loader-0.16.9-1.21.1.jar")));
        assertTrue(Files.exists(profile.path().resolve("natives-linux").resolve("lwjgl.so")));
        assertFalse(Files.exists(newPath.resolve("fabric-loader-0.16.9-1.21.1.json")));
        assertFalse(Files.exists(newPath.resolve("natives-linux")));
    }

    @Test
    void aGameCanBeMovedIntoTheDirectoryOfAVersionNothingIsPlayedIn() {
        Profile profile = withFiles(profile("main"));
        Path versionDir = inVersionDirectory("unused", "fabric-loader-0.16.9-1.21.1").path();
        profileService.remove(profileService.getProfile("unused").orElseThrow());

        ProfileMover.Result result = move(profile, versionDir, null);

        assertTrue(result.filesMoved());
        assertEquals("options of main", read(versionDir.resolve("options.txt")));
        assertEquals("level", read(versionDir.resolve("saves").resolve("world").resolve("level.dat")));
        // next to the version, which is untouched
        assertEquals("json", read(versionDir.resolve("fabric-loader-0.16.9-1.21.1.json")));
        assertEquals("jar", read(versionDir.resolve("fabric-loader-0.16.9-1.21.1.jar")));
        assertFalse(Files.exists(profile.path()));
    }

    @Test
    void aGameIsNotMovedIntoAVersionDirectoryAnotherGameIsPlayedIn() {
        Profile profile = withFiles(profile("main"));
        Profile other = withFiles(inVersionDirectory("other", "fabric-loader-0.16.9-1.21.1"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> move(profile, other.path(), null));

        assertTrue(e.getMessage().contains("is not empty"), e.getMessage());
        assertEquals("options of main", read(profile.path().resolve("options.txt")));
        assertEquals("options of other", read(other.path().resolve("options.txt")));
    }

    @Test
    void aDirectoryInVersionsWithoutAVersionIsMovedAsAWhole() {
        // e.g. the placeholder of a version that never got installed, there is nothing to leave behind
        Path dir = mcFiles.getVersionsDir().resolve("fabric-1.21.1");
        Profile profile = withFiles(profileService.save(
            new Profile("main", Optional.of(Side.CLIENT), base.handle("x"), dir)
        ));
        Path newPath = root.resolve("elsewhere").resolve("main");

        move(profile, newPath, null);

        assertFalse(Files.exists(dir));
        assertEquals("options of main", read(newPath.resolve("options.txt")));
    }

}
