package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.launcher.backup.*;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackupCommandsTest {
    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();
    private final BackupFormatter formatter = new BackupFormatter();

    private TestBackupEnvironment env;
    private BackupService backupService;
    private Profile server;

    @BeforeEach
    void setup() throws IOException {
        env = new TestBackupEnvironment(root);
        env.getConfig().setIgnore(List.of("logs"));
        backupService = env.getBackupService();

        Profile client = env.getProfileService().createDefault("main", VersionArg.parse("fabric", "1.21.1"));
        write(client.path().resolve("options.txt"), "fov:70");
        write(client.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        write(client.path().resolve("saves").resolve("B").resolve("level.dat"), "b");

        server = env.getProfileService().createDefault("srv", VersionArg.parse("server", "vanilla", "1.21.1"));
        write(server.path().resolve("server.jar"), "jar");
        write(server.path().resolve("world").resolve("level.dat"), "w");
    }

    private void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private BackupCommand backup(String... args) {
        BackupCommand command = new BackupCommand(backupService, formatter, console);
        command.setTarget(List.of(args));
        return command;
    }

    private Backup only() {
        List<Backup> backups = backupService.list();
        assertEquals(1, backups.size());
        return backups.getFirst();
    }

    private DiffCommand diff(String backup) {
        DiffCommand command = new DiffCommand(backupService, tables, console);
        command.setBackup(backup);
        return command;
    }

    private RestoreCommand restore(String id) {
        RestoreCommand command = new RestoreCommand(backupService, console);
        command.setId(id);
        return command;
    }

    private PruneCommand prune() {
        return new PruneCommand(backupService, formatter, console);
    }

    @Test
    void backupWithoutTargetThrows() {
        assertThrows(IllegalArgumentException.class, backup()::run);
    }

    @Test
    void backupDefaultsToFullProfileBackup() {
        backup("main").run();

        Backup backup = only();
        assertEquals("profile", backup.provider());
        assertEquals("full", backup.type());
        assertEquals("main", backup.target());
        assertEquals(BackupOrigin.MANUAL, backup.origin());
        assertTrue(console.output().contains("Created full backup " + backup.id() + " of main"));
    }

    @Test
    void backupResolvesVersionArgs() {
        backup("server", "vanilla", "1.21.1").run();

        assertEquals("srv", only().target());
    }

    @Test
    void backupWorldDefaultsToWorldType() {
        BackupCommand command = backup("main");
        command.setWorld("B");
        command.run();

        assertEquals("world", only().type());
        assertEquals(List.of("saves/B"), only().paths());
    }

    @Test
    void backupWorldsOfServer() {
        BackupCommand command = backup("srv");
        command.setType("WORLDS");
        command.run();

        assertEquals("worlds", only().type());
        assertEquals(List.of("world"), only().paths());
    }

    @Test
    void worldWithOtherTypeThrows() {
        BackupCommand command = backup("main");
        command.setType("full");
        command.setWorld("A");

        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void unknownProviderThrows() {
        BackupCommand command = backup("main");
        command.setProvider("container");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("available: profile"));
    }

    @Test
    void backupWithIgnoredPaths() throws IOException {
        write(server.path().resolve("logs").resolve("latest.log"), "log");
        write(server.path().resolve("cache").resolve("data.bin"), "bin");

        BackupCommand command = backup("srv");
        command.setIgnore(List.of("cache"));
        command.run();

        assertEquals(List.of("logs", "cache"), only().ignored());
        assertEquals(2, only().files());
    }

    @Test
    void backupWithoutDefaultIgnores() throws IOException {
        write(server.path().resolve("logs").resolve("latest.log"), "log");

        BackupCommand command = backup("srv");
        command.setNoDefaultIgnore(true);
        command.run();

        assertEquals(List.of(), only().ignored());
        assertEquals(3, only().files());
    }

    @Test
    void backupOfRunningServerRequiresForce() {
        env.getProcessManager().add("srv", server.path());

        assertThrows(BackupInUseException.class, backup("srv")::run);

        BackupCommand command = backup("srv");
        command.setForce(true);
        command.run();
        assertEquals(1, backupService.list().size());
    }

    @Test
    void listShowsBackups() {
        backup("main").run();
        backup("srv").run();

        new ListCommand(backupService, formatter, tables, console).run();
        assertTrue(console.output().contains("main-2026-10-28_12-30-00"));
        assertTrue(console.output().contains("srv-2026-10-28_12-30-00"));
        assertTrue(console.output().contains("manual"));
        assertFalse(console.output().contains("provider"));

        RecordingConsole filtered = new RecordingConsole();
        ListCommand command = new ListCommand(backupService, formatter, tables, filtered);
        command.setTarget("srv");
        command.run();
        assertFalse(filtered.output().contains("main-2026-10-28_12-30-00"));
        assertTrue(filtered.output().contains("srv-2026-10-28_12-30-00"));
    }

    @Test
    void diffWithoutChanges() {
        backup("srv").run();

        diff("srv-2026-10-28_12-30-00").run();
        assertTrue(console.output().contains("No differences"));
    }

    @Test
    void diffShowsChangesOfLatestBackupOfProfile() throws IOException {
        backup("srv").run();
        write(server.path().resolve("world").resolve("level.dat"), "changed");
        write(server.path().resolve("ops.json"), "[]");

        DiffCommand command = diff("srv");
        command.setChecksum(true);
        command.run();
        String output = console.output();
        assertTrue(output.contains("modified"));
        assertTrue(output.contains("world/level.dat"));
        assertTrue(output.contains("ops.json"));
        assertTrue(output.contains("1 added, 0 removed, 1 modified"));
    }

    @Test
    void diffUnknownBackupThrows() {
        assertThrows(IllegalArgumentException.class, diff("nope")::run);
    }

    @Test
    void restoreRevertsChangesAndBacksUpFirst() throws IOException {
        backup("srv").run();
        write(server.path().resolve("world").resolve("level.dat"), "changed");

        restore(only().id()).run();

        assertEquals("w", Files.readString(server.path().resolve("world").resolve("level.dat")));
        assertEquals(2, backupService.list().size());
        assertTrue(console.output().contains("Backed up the current files as srv-2026-10-28_12-30-00-2"));
        assertTrue(console.output().contains("deleted 0, restored 0 and reverted 1 files"));
    }

    @Test
    void restoreWithoutBackupKeepingNewFiles() throws IOException {
        backup("srv").run();
        Backup backup = only();
        Files.delete(server.path().resolve("server.jar"));
        write(server.path().resolve("ops.json"), "[]");

        RestoreCommand command = restore(backup.id());
        command.setNoBackup(true);
        command.setKeepNew(true);
        command.run();

        assertTrue(Files.exists(server.path().resolve("server.jar")));
        assertTrue(Files.exists(server.path().resolve("ops.json")));
        assertEquals(List.of(backup), backupService.list());
    }

    @Test
    void restoreOfRunningServerRequiresForce() throws IOException {
        backup("srv").run();
        Backup backup = only();
        write(server.path().resolve("world").resolve("level.dat"), "changed");
        env.getProcessManager().add("srv", server.path());

        assertThrows(BackupInUseException.class, restore(backup.id())::run);

        RestoreCommand command = restore(backup.id());
        command.setForce(true);
        command.run();
        assertEquals("w", Files.readString(server.path().resolve("world").resolve("level.dat")));
    }

    @Test
    void restoreWithoutChanges() {
        backup("srv").run();

        restore(only().id()).run();

        assertTrue(console.output().contains("already matches backup"));
        assertEquals(1, backupService.list().size());
    }

    @Test
    void restoreUnknownBackupThrows() {
        assertThrows(IllegalArgumentException.class, restore("nope")::run);
    }

    @Test
    void removeDeletesBackup() {
        backup("main").run();
        Backup backup = only();

        RemoveCommand command = new RemoveCommand(backupService, console);
        command.setId(backup.id());
        command.run();

        assertEquals(List.of(), backupService.list());
        assertFalse(Files.exists(backupService.getArchive(backup)));
        assertTrue(console.output().contains("Removed backup " + backup.id()));
    }

    @Test
    void removeUnknownBackupThrows() {
        RemoveCommand command = new RemoveCommand(backupService, console);
        command.setId("nope");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void pruneKeepsNewestBackups() {
        backup("srv").run();
        env.getClock().advance(Duration.ofDays(10));
        backup("srv").run();
        backup("main").run();

        PruneCommand command = prune();
        command.setTarget("srv");
        command.setKeep(1);
        command.run();

        assertEquals(List.of("main", "srv"), backupService.list().stream().map(Backup::target).sorted().toList());
        assertTrue(console.output().contains("Removed backup srv-2026-10-28_12-30-00"));
        assertTrue(console.output().contains("Removed 1 backups."));
    }

    @Test
    void pruneOlderThanByOrigin() {
        backup("srv").run();
        backupService.create(new BackupRequest(
            "profile", "srv", null, null, null, List.of(), true, BackupOrigin.LAUNCH, false
        ));
        env.getClock().advance(Duration.ofDays(31));

        PruneCommand command = prune();
        command.setOlderThan("30d");
        command.setOrigins(List.of("launch"));
        command.run();

        assertEquals(List.of(BackupOrigin.MANUAL), backupService.list().stream().map(Backup::origin).toList());
    }

    @Test
    void pruneRequiresKeepOrAge() {
        assertThrows(IllegalArgumentException.class, prune()::run);
    }

    @Test
    void formatsSizes() {
        assertEquals("512 B", formatter.size(512));
        assertEquals("1.5 KiB", formatter.size(1536));
        assertEquals("2.0 MiB", formatter.size(2 * 1024 * 1024));
    }

    @Test
    void parsesDurations() {
        assertEquals(Duration.ofDays(30), formatter.duration("30d"));
        assertEquals(Duration.ofDays(14), formatter.duration("2W"));
        assertEquals(Duration.ofHours(36), formatter.duration("1d12h"));
        assertEquals(Duration.ofSeconds(90), formatter.duration("90s"));
        assertThrows(IllegalArgumentException.class, () -> formatter.duration("30"));
        assertThrows(IllegalArgumentException.class, () -> formatter.duration("30x"));
        assertThrows(IllegalArgumentException.class, () -> formatter.duration("d30d"));
    }

}
