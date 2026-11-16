package io.github.headlesshq.headlessmc.launcher.backup.profile;

import io.github.headlesshq.headlessmc.launcher.backup.Backup;
import io.github.headlesshq.headlessmc.launcher.backup.BackupOrigin;
import io.github.headlesshq.headlessmc.launcher.backup.BackupRequest;
import io.github.headlesshq.headlessmc.launcher.backup.TestBackupEnvironment;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMoveEvent;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMovedEvent;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Moving and renaming profiles: files that are in use are not moved, and backups follow a rename.
 */
class ProfileMoveObserverTest {
    @TempDir
    Path root;

    private TestBackupEnvironment env;
    private ProfileMoveObserver observer;

    @BeforeEach
    void setup() {
        env = new TestBackupEnvironment(root);
        observer = new ProfileMoveObserver(
            new ProfileUsageService(env.getProcessManager(), env.getWorldService()),
            env.getProvider(),
            env.getBackupService()
        );
    }

    private Profile server() throws IOException {
        Profile profile = env.getProfileService()
            .save(new Profile("srv", VersionArg.parse("server", "1.21.1"), root.resolve("srv")));
        Files.createDirectories(profile.path());
        Files.writeString(profile.path().resolve("server.jar"), "jar");
        return profile;
    }

    private Backup backup(Profile profile) {
        return env.getBackupService().create(
            new BackupRequest("profile", profile.name(), null, null, null, List.of(), true, BackupOrigin.MANUAL, false)
        );
    }

    // before a move

    @Test
    void aRunningProfileIsNotMoved() throws IOException {
        Profile profile = server();
        env.getProcessManager().add("server", profile.path());

        IllegalStateException e = assertThrows(
            IllegalStateException.class,
            () -> observer.beforeMove(new ProfileMoveEvent(profile, "srv", root.resolve("elsewhere")))
        );

        assertTrue(e.getMessage().contains("srv is running"), e.getMessage());
        assertTrue(e.getMessage().contains("stop it before moving profile srv."), e.getMessage());
    }

    @Test
    void aRunningProfileCanBeRenamed() throws IOException {
        Profile profile = server();
        env.getProcessManager().add("server", profile.path());

        // renaming does not touch the files of the game
        assertDoesNotThrow(() -> observer.beforeMove(new ProfileMoveEvent(profile, "survival", profile.path())));
    }

    @Test
    void aProfileWithAWorldThatIsOpenIsNotMoved() throws IOException {
        Profile profile = env.getProfileService()
            .save(new Profile("Main", VersionArg.parse("fabric", "1.21.1"), root.resolve("main")));
        Path world = Files.createDirectories(profile.path().resolve("saves").resolve("A"));
        Files.writeString(world.resolve("level.dat"), "a");
        Path lock = Files.writeString(world.resolve("session.lock"), "lock");
        ProfileMoveEvent event = new ProfileMoveEvent(profile, "Main", root.resolve("elsewhere"));

        // e.g. the game is running in another launcher
        try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.WRITE);
             FileLock _ = channel.lock()) {
            assertThrows(IllegalStateException.class, () -> observer.beforeMove(event));
        }

        assertDoesNotThrow(() -> observer.beforeMove(event));
    }

    @Test
    void aProfileThatIsNotInUseCanBeMoved() throws IOException {
        Profile profile = server();
        // another profile is running
        env.getProcessManager().add("other", root.resolve("other"));

        assertDoesNotThrow(() -> observer.beforeMove(new ProfileMoveEvent(profile, "srv", root.resolve("elsewhere"))));
    }

    @Test
    void aProfileWithoutAGameDirectoryCanBeMoved() {
        // never launched, there is nothing to move
        Profile profile = new Profile("new", VersionArg.parse("server", "1.21.1"), root.resolve("new"));
        env.getProcessManager().add("server", profile.path());

        assertDoesNotThrow(() -> observer.beforeMove(new ProfileMoveEvent(profile, "new", root.resolve("elsewhere"))));
    }

    // after a move

    @Test
    void backupsFollowARename() throws IOException {
        Profile profile = server();
        Backup backup = backup(profile);
        Profile renamed = profile.withName("survival");

        observer.afterMove(new ProfileMovedEvent(profile, renamed));

        assertEquals(List.of(), env.getBackupService().list("profile", "srv"));
        List<Backup> backups = env.getBackupService().list("profile", "survival");
        assertEquals(List.of(backup.id()), backups.stream().map(Backup::id).toList());
        assertEquals("survival", backups.getFirst().profile().name());
        // and they are found for the renamed profile
        assertTrue(Files.exists(env.getBackupService().getArchive(backups.getFirst())));
    }

    @Test
    void backupsStayWhereTheyAreWhenOnlyTheGameDirectoryMoves() throws IOException {
        Profile profile = server();
        Backup backup = backup(profile);

        observer.afterMove(new ProfileMovedEvent(profile, profile.withPath(root.resolve("elsewhere"))));

        assertEquals(List.of(backup), env.getBackupService().list());
    }

    @Test
    void renamingAProfileWithoutBackupsDoesNothing() throws IOException {
        Profile profile = server();

        assertDoesNotThrow(() -> observer.afterMove(new ProfileMovedEvent(profile, profile.withName("survival"))));
        assertEquals(List.of(), env.getBackupService().list());
    }

}
