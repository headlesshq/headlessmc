package io.github.headlesshq.headlessmc.launcher.backup.profile;

import io.github.headlesshq.headlessmc.launcher.backup.Backup;
import io.github.headlesshq.headlessmc.launcher.backup.BackupOrigin;
import io.github.headlesshq.headlessmc.launcher.backup.TestBackupEnvironment;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileLaunchEvent;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AutoBackupServiceTest {
    @TempDir
    Path root;

    private TestBackupEnvironment env;
    private AutoBackupService service;
    private Profile profile;

    @BeforeEach
    void setup() throws IOException {
        env = new TestBackupEnvironment(root);
        service = env.getAutoBackupService();
        profile = new Profile("main", VersionArg.parse("fabric", "1.21.1"), root.resolve("main"));
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        write(profile.path().resolve("options.txt"), "fov:70");
    }

    private void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @Test
    void disabledByDefault() {
        assertEquals(Optional.empty(), service.backupBeforeLaunch(profile));
        assertEquals(List.of(), env.getBackupService().list());
    }

    @Test
    void backsUpWorldsBeforeLaunch() {
        env.getConfig().setAuto("worlds");

        Backup backup = service.backupBeforeLaunch(profile).orElseThrow();

        assertEquals(BackupOrigin.LAUNCH, backup.origin());
        assertEquals("worlds", backup.type());
        assertEquals(List.of("saves/A"), backup.paths());
        assertEquals(profile, backup.profile());
        assertTrue(env.getConsole().output().contains("Backed up main before launching"));
    }

    @Test
    void backsUpProfilesThatAreNotSaved() {
        env.getConfig().setAuto("FULL");

        Backup backup = service.backupBeforeLaunch(profile).orElseThrow();

        assertEquals("full", backup.type());
        assertEquals("main", backup.target());
        assertTrue(env.getProfileService().getProfile("main").isEmpty());
    }

    @Test
    void skipsUnchangedProfiles() throws IOException {
        env.getConfig().setAuto("worlds");
        Backup first = service.backupBeforeLaunch(profile).orElseThrow();
        env.getClock().advance(Duration.ofSeconds(1));

        assertEquals(Optional.empty(), service.backupBeforeLaunch(profile));

        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "changed");
        Backup second = service.backupBeforeLaunch(profile).orElseThrow();
        assertNotEquals(first.id(), second.id());

        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "b");
        env.getClock().advance(Duration.ofSeconds(1));
        assertTrue(service.backupBeforeLaunch(profile).isPresent());
    }

    @Test
    void skipsProfilesWithoutWorlds() {
        env.getConfig().setAuto("worlds");
        Profile empty = new Profile("empty", VersionArg.parse("fabric", "1.21.1"), root.resolve("empty"));

        assertEquals(Optional.empty(), service.backupBeforeLaunch(empty));
    }

    @Test
    void skipsRunningProfiles() {
        env.getConfig().setAuto("worlds");
        env.getProcessManager().add("main", profile.path());

        assertEquals(Optional.empty(), service.backupBeforeLaunch(profile));
        assertEquals(List.of(), env.getBackupService().list());
    }

    @Test
    void invalidConfigurationDisablesBackups() {
        env.getConfig().setAuto("world");

        assertEquals(Optional.empty(), service.backupBeforeLaunch(profile));
    }

    @Test
    void launchEventTriggersBackup() {
        env.getConfig().setAuto("worlds");

        service.onLaunch(new ProfileLaunchEvent(profile, false));
        assertEquals(List.of(), env.getBackupService().list());

        service.onLaunch(new ProfileLaunchEvent(profile, true));
        assertEquals(1, env.getBackupService().list().size());
    }

    @Test
    void keepsConfiguredAmountOfLaunchBackups() throws IOException {
        env.getConfig().setAuto("worlds");
        env.getConfig().setAutoKeep(2);
        for (int i = 0; i < 4; i++) {
            write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "version" + "!".repeat(i));
            service.backupBeforeLaunch(profile).orElseThrow();
            env.getClock().advance(Duration.ofSeconds(1));
        }

        assertEquals(2, env.getBackupService().list().size());
    }

}
