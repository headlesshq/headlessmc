package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceImplTest {
    @TempDir
    Path root;

    private TestBackupEnvironment env;
    private BackupServiceImpl service;

    @BeforeEach
    void setup() {
        env = new TestBackupEnvironment(root, new DirectoryProvider());
        service = env.getBackupService();
    }

    private Profile client() {
        return env.getProfileService().save(new Profile("Main", VersionArg.parse("fabric", "1.21.1"), root.resolve("main")));
    }

    private Profile server() {
        return env.getProfileService().save(new Profile("srv", VersionArg.parse("server", "1.21.1"), root.resolve("srv")));
    }

    private Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    private BackupRequest request(Profile profile, @Nullable String type, @Nullable String world) {
        return new BackupRequest("profile", profile.name(), null, type, world, List.of(), true, BackupOrigin.MANUAL, false);
    }

    private Backup create(Profile profile, @Nullable String type, @Nullable String world) {
        return service.create(request(profile, type, world));
    }

    private Backup create(Profile profile, BackupOrigin origin) {
        return service.create(new BackupRequest("profile", profile.name(), null, null, null, List.of(), true, origin, false));
    }

    private Backup createIgnoring(Profile profile, @Nullable String type, @Nullable String world, List<String> ignore) {
        return service.create(new BackupRequest("profile", profile.name(), null, type, world, ignore, true, BackupOrigin.MANUAL, false));
    }

    private List<String> entries(Backup backup) throws IOException {
        try (ZipFile zip = new ZipFile(service.getArchive(backup).toFile())) {
            return zip.stream().map(entry -> entry.getName()).sorted().toList();
        }
    }

    @Test
    void fullClientBackupSkipsDownloadableDirectories() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("options.txt"), "fov:70");
        write(profile.path().resolve("mods").resolve("a.jar"), "a");
        write(profile.path().resolve("versions").resolve("1.21.1").resolve("1.21.1.jar"), "jar");
        write(profile.path().resolve("assets").resolve("index.json"), "{}");
        write(profile.path().resolve("logs").resolve("latest.log"), "log");
        write(profile.path().resolve("saves").resolve("w").resolve("session.lock"), "lock");

        Backup backup = create(profile, null, null);

        assertEquals("main-2026-10-28_12-30-00", backup.id());
        assertEquals("profile", backup.provider());
        assertEquals("Main", backup.target());
        assertEquals("full", backup.type());
        assertEquals(BackupOrigin.MANUAL, backup.origin());
        assertEquals(profile, backup.profile());
        assertEquals(profile.path(), backup.source());
        assertEquals(List.of(""), backup.paths());
        assertEquals(List.of("logs", "crash-reports", "assets", "libraries", "versions"), backup.ignored());
        assertEquals(2, backup.files());
        assertEquals(List.of("mods/a.jar", "options.txt"), entries(backup));
        assertTrue(Files.exists(root.resolve("headlessmc").resolve("backups").resolve("profile").resolve("main")
            .resolve(backup.id() + ".json")));
    }

    @Test
    void fullServerBackupKeepsLibraries() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        write(profile.path().resolve("libraries").resolve("lib.jar"), "lib");

        Backup backup = create(profile, null, null);

        assertEquals(List.of("logs", "crash-reports"), backup.ignored());
        assertEquals(List.of("libraries/lib.jar", "server.jar"), entries(backup));
    }

    @Test
    void worldsBackupOfClientContainsAllSaves() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "b");
        write(profile.path().resolve("options.txt"), "fov:70");

        Backup backup = create(profile, "worlds", null);

        assertEquals(List.of("saves/A", "saves/B"), backup.paths());
        assertEquals(List.of("saves/A/level.dat", "saves/B/level.dat"), entries(backup));
    }

    @Test
    void worldsBackupOfServerContainsWorldDirectories() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("world").resolve("level.dat"), "overworld");
        write(profile.path().resolve("world_nether").resolve("level.dat"), "nether");
        write(profile.path().resolve("logs").resolve("latest.log"), "log");

        Backup backup = create(profile, "worlds", null);

        assertEquals(List.of("world", "world_nether"), backup.paths());
        assertEquals(List.of("world/level.dat", "world_nether/level.dat"), entries(backup));
    }

    @Test
    void worldsBackupWithoutWorldsThrows() throws IOException {
        Profile profile = client();
        Files.createDirectories(profile.path());

        assertThrows(IllegalArgumentException.class, () -> create(profile, "worlds", null));
    }

    @Test
    void worldBackupContainsSingleWorld() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "b");

        Backup backup = create(profile, null, "B");

        assertEquals("world", backup.type());
        assertEquals(List.of("saves/B"), backup.paths());
        assertEquals(List.of("saves/B/level.dat"), entries(backup));
    }

    @Test
    void worldBackupOfServerUsesLevelName() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.properties"), "level-name=survival\n");
        write(profile.path().resolve("survival").resolve("level.dat"), "s");
        write(profile.path().resolve("world").resolve("level.dat"), "w");

        assertEquals(List.of("survival"), create(profile, "world", null).paths());
    }

    @Test
    void worldWithOtherTypeThrows() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");

        assertThrows(IllegalArgumentException.class, () -> create(profile, "full", "A"));
    }

    @Test
    void unknownTypeThrows() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> create(profile, "everything", null));
        assertTrue(e.getMessage().contains("full, worlds, world"));
    }

    @Test
    void unknownProviderThrows() {
        BackupRequest request = new BackupRequest("nope", "x", null, null, null, List.of(), true, BackupOrigin.MANUAL, false);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.create(request));
        assertTrue(e.getMessage().contains("directory, profile"));
    }

    @Test
    void backupOfMissingGameDirThrows() {
        Profile profile = client();
        assertThrows(IllegalArgumentException.class, () -> create(profile, null, null));
    }

    @Test
    void backupDirInsideGameDirIsSkipped() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        env.getConfig().setDir(Optional.of(profile.path().resolve("backups").toString()));

        Backup first = create(profile, null, null);
        Backup second = create(profile, null, null);

        assertEquals(List.of("server.jar"), entries(second));
        assertTrue(service.getArchive(first).startsWith(profile.path().resolve("backups")));
        assertTrue(service.diff(second, true).isEmpty());
    }

    @Test
    void idsAreUnique() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");

        assertEquals("srv-2026-10-28_12-30-00", create(profile, null, null).id());
        assertEquals("srv-2026-10-28_12-30-00-2", create(profile, null, null).id());
    }

    @Test
    void listReadsBackups() throws IOException {
        write(client().path().resolve("options.txt"), "fov:70");
        write(server().path().resolve("server.jar"), "jar");

        Backup clientBackup = create(client(), null, null);
        env.getClock().advance(Duration.ofSeconds(1));
        Backup serverBackup = create(server(), null, null);

        assertEquals(List.of(clientBackup, serverBackup), service.list());
        assertEquals(List.of(serverBackup), service.list(null, "SRV"));
        assertEquals(List.of(serverBackup), service.list("profile", "srv"));
        assertEquals(List.of(), service.list("directory", null));
        assertEquals(Optional.of(clientBackup), service.get(clientBackup.id()));
        assertEquals(Optional.empty(), service.get("nope"));
    }

    @Test
    void listWithoutBackupDirIsEmpty() {
        assertEquals(List.of(), service.list());
    }

    @Test
    void diffDetectsChanges() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        write(profile.path().resolve("ops.json"), "[]");
        write(profile.path().resolve("world").resolve("level.dat"), "level");

        Backup backup = create(profile, null, null);
        assertTrue(service.diff(backup, true).isEmpty());

        write(profile.path().resolve("ops.json"), "[\"me\"]");
        write(profile.path().resolve("world").resolve("level.dat"), "LEVEL");
        Files.delete(profile.path().resolve("server.jar"));
        write(profile.path().resolve("whitelist.json"), "[]");

        BackupDiff diff = service.diff(backup, true);
        assertEquals(List.of("whitelist.json"), diff.added());
        assertEquals(List.of("server.jar"), diff.removed());
        assertEquals(List.of("ops.json", "world/level.dat"), diff.modified());
    }

    @Test
    void fastDiffTrustsSizeAndModificationTime() throws IOException {
        Profile profile = server();
        Path level = write(profile.path().resolve("world").resolve("level.dat"), "level");
        FileTime time = Files.getLastModifiedTime(level);

        Backup backup = create(profile, null, null);
        write(level, "LEVEL");
        Files.setLastModifiedTime(level, time);

        assertTrue(service.diff(backup, false).isEmpty());
        assertEquals(List.of("world/level.dat"), service.diff(backup, true).modified());

        write(level, "longer level");
        assertEquals(List.of("world/level.dat"), service.diff(backup, false).modified());
    }

    @Test
    void diffOfWorldBackupIgnoresOtherFiles() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "b");

        Backup backup = create(profile, "world", "A");
        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "changed");
        write(profile.path().resolve("options.txt"), "fov:70");

        assertTrue(service.diff(backup, true).isEmpty());
    }

    @Test
    void diffOfWorldsBackupContainsNewWorlds() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");

        Backup backup = create(profile, "worlds", null);
        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "b");

        assertEquals(List.of("saves/B/level.dat"), service.diff(backup, true).added());
    }

    @Test
    void diffIgnoresIgnoredFiles() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("options.txt"), "fov:70");

        Backup backup = create(profile, null, null);
        write(profile.path().resolve("versions").resolve("1.21.1.jar"), "jar");
        write(profile.path().resolve("logs").resolve("latest.log"), "log");

        assertTrue(service.diff(backup, true).isEmpty());
    }

    @Test
    void ignoredGlobs() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        write(profile.path().resolve("debug").resolve("a.log"), "log");
        write(profile.path().resolve("cache").resolve("x").resolve("y.bin"), "bin");
        write(profile.path().resolve("world").resolve("level.dat"), "level");
        write(profile.path().resolve("world").resolve("region").resolve("r.0.0.mca.tmp"), "tmp");

        Backup backup = createIgnoring(profile, null, null, List.of("**/*.log", "cache/", "world/region/*.tmp"));

        assertEquals(List.of("server.jar", "world/level.dat"), entries(backup));
    }

    @Test
    void ignoredGlobsAreConfigurable() throws IOException {
        env.getConfig().setIgnore(List.of("*.json", " "));
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        write(profile.path().resolve("ops.json"), "[]");
        write(profile.path().resolve("logs").resolve("latest.log"), "log");

        Backup backup = create(profile, null, null);

        assertEquals(List.of("*.json"), backup.ignored());
        assertEquals(List.of("logs/latest.log", "server.jar"), entries(backup));
    }

    @Test
    void defaultIgnoresCanBeDisabled() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("logs").resolve("latest.log"), "log");
        write(profile.path().resolve("versions").resolve("1.21.1.jar"), "jar");

        Backup backup = service.create(new BackupRequest(
            "profile", "Main", null, null, null, List.of("versions"), false, BackupOrigin.MANUAL, false
        ));

        assertEquals(List.of("versions"), backup.ignored());
        assertEquals(List.of("logs/latest.log"), entries(backup));
    }

    @Test
    void ignoredPathsInsideWorldBackups() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        write(profile.path().resolve("saves").resolve("A").resolve("stats").resolve("me.json"), "{}");

        Backup backup = createIgnoring(profile, null, "A", List.of("saves/*/stats"));

        assertEquals(List.of("saves/A/level.dat"), entries(backup));
        write(profile.path().resolve("saves").resolve("A").resolve("stats").resolve("me.json"), "{\"changed\": true}");
        assertTrue(service.diff(backup, true).isEmpty());
    }

    @Test
    void runningProfileIsInUse() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        env.getProcessManager().add("server", profile.path());

        BackupInUseException e = assertThrows(BackupInUseException.class, () -> create(profile, null, null));
        assertTrue(e.getMessage().contains("srv is running"));

        Backup backup = service.create(new BackupRequest(
            "profile", "srv", null, null, null, List.of(), true, BackupOrigin.MANUAL, true
        ));
        assertEquals(1, backup.files());
        assertThrows(BackupInUseException.class, () -> service.restore(backup, RestoreOptions.DEFAULT));
    }

    @Test
    void lockedWorldIsInUse() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        Path lock = write(profile.path().resolve("saves").resolve("A").resolve("session.lock"), "lock");
        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "b");

        try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.WRITE);
             FileLock _ = channel.lock()) {
            assertThrows(BackupInUseException.class, () -> create(profile, null, null));
            assertThrows(BackupInUseException.class, () -> create(profile, null, "A"));
            assertEquals(List.of("saves/B"), create(profile, null, "B").paths());
        }

        assertEquals(List.of("saves/A"), create(profile, null, "A").paths());
    }

    @Test
    void restoreRevertsChanges() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        write(profile.path().resolve("ops.json"), "[]");
        write(profile.path().resolve("world").resolve("level.dat"), "level");
        Backup backup = create(profile, null, null);

        write(profile.path().resolve("ops.json"), "[\"me\"]");
        Files.delete(profile.path().resolve("server.jar"));
        write(profile.path().resolve("world").resolve("new").resolve("data.dat"), "new");
        write(profile.path().resolve("logs").resolve("latest.log"), "log");
        env.getProfileService().save(profile.withVmArgs(List.of("-Xmx4G")));

        RestoreResult result = service.restore(backup, new RestoreOptions(false, false, false));

        assertEquals(List.of("world/new/data.dat"), result.changes().added());
        assertEquals(List.of("server.jar"), result.changes().removed());
        assertEquals(List.of("ops.json"), result.changes().modified());
        assertNull(result.safetyBackup());
        assertTrue(service.diff(backup, true).isEmpty());
        assertEquals("[]", Files.readString(profile.path().resolve("ops.json")));
        assertEquals("jar", Files.readString(profile.path().resolve("server.jar")));
        assertFalse(Files.exists(profile.path().resolve("world").resolve("new")));
        assertTrue(Files.exists(profile.path().resolve("world")));
        assertTrue(Files.exists(profile.path().resolve("logs").resolve("latest.log")));
        assertEquals(Optional.of(profile), env.getProfileService().getProfile("srv"));
    }

    @Test
    void restoreCanKeepNewFiles() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("ops.json"), "[]");
        Backup backup = create(profile, null, null);
        write(profile.path().resolve("ops.json"), "[\"me\"]");
        write(profile.path().resolve("whitelist.json"), "[]");

        RestoreResult result = service.restore(backup, new RestoreOptions(false, true, false));

        assertEquals(List.of(), result.changes().added());
        assertEquals(List.of("ops.json"), result.changes().modified());
        assertTrue(Files.exists(profile.path().resolve("whitelist.json")));
        assertEquals("[]", Files.readString(profile.path().resolve("ops.json")));
    }

    @Test
    void restoreCreatesSafetyBackup() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        Backup backup = create(profile, null, "A");
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "changed");

        RestoreResult result = service.restore(backup, RestoreOptions.DEFAULT);

        Backup safetyBackup = result.safetyBackup();
        assertNotNull(safetyBackup);
        assertEquals(BackupOrigin.RESTORE, safetyBackup.origin());
        assertEquals("world", safetyBackup.type());
        assertEquals(List.of("saves/A"), safetyBackup.paths());
        assertEquals("a", Files.readString(profile.path().resolve("saves").resolve("A").resolve("level.dat")));
        assertEquals(List.of("saves/A/level.dat"), service.diff(safetyBackup, true).modified());
        assertEquals(List.of(backup, safetyBackup), service.list());
    }

    @Test
    void restoreWithoutChangesDoesNothing() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        Backup backup = create(profile, null, null);

        RestoreResult result = service.restore(backup, RestoreOptions.DEFAULT);

        assertTrue(result.changes().isEmpty());
        assertNull(result.safetyBackup());
        assertEquals(List.of(backup), service.list());
    }

    @Test
    void restoreOfWorldsKeepsNewWorlds() throws IOException {
        Profile profile = client();
        write(profile.path().resolve("saves").resolve("A").resolve("level.dat"), "a");
        Backup backup = create(profile, "worlds", null);
        Files.delete(profile.path().resolve("saves").resolve("A").resolve("level.dat"));
        write(profile.path().resolve("saves").resolve("B").resolve("level.dat"), "b");

        service.restore(backup, new RestoreOptions(false, false, false));

        assertEquals("a", Files.readString(profile.path().resolve("saves").resolve("A").resolve("level.dat")));
        assertTrue(Files.exists(profile.path().resolve("saves").resolve("B").resolve("level.dat")));
    }

    @Test
    void restoreIntoDeletedGameDir() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("world").resolve("level.dat"), "level");
        Backup backup = create(profile, null, null);
        env.getFileService().delete(profile.path());

        RestoreResult result = service.restore(backup, RestoreOptions.DEFAULT);

        assertNull(result.safetyBackup());
        assertEquals("level", Files.readString(profile.path().resolve("world").resolve("level.dat")));
    }

    @Test
    void removeDeletesBackup() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");

        Backup first = create(profile, null, null);
        Backup second = create(profile, null, null);
        Path dir = service.getArchive(first).getParent();

        service.remove(first);
        assertFalse(Files.exists(service.getArchive(first)));
        assertEquals(List.of(second), service.list());

        service.remove(second);
        assertFalse(Files.exists(dir));
        assertEquals(List.of(), service.list());
    }

    @Test
    void pruneKeepsNewestBackupsOfEachTarget() throws IOException {
        write(server().path().resolve("server.jar"), "jar");
        write(client().path().resolve("options.txt"), "fov:70");
        Backup oldServer = create(server(), null, null);
        Backup oldClient = create(client(), null, null);
        env.getClock().advance(Duration.ofMinutes(1));
        Backup newServer = create(server(), null, null);
        Backup newClient = create(client(), null, null);

        assertEquals(List.of(oldServer, oldClient), service.prune(new PruneRequest(null, null, Set.of(), 1, null)).stream()
            .sorted(java.util.Comparator.comparing(Backup::target).reversed()).toList());
        assertEquals(List.of(newServer, newClient), service.list().stream()
            .sorted(java.util.Comparator.comparing(Backup::target).reversed()).toList());
    }

    @Test
    void pruneOlderThan() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        Backup old = create(profile, null, null);
        env.getClock().advance(Duration.ofDays(10));
        Backup recent = create(profile, null, null);

        assertEquals(List.of(old), service.prune(new PruneRequest("profile", "srv", Set.of(), null, Duration.ofDays(7))));
        assertEquals(List.of(recent), service.list());
        assertEquals(List.of(), service.prune(new PruneRequest("profile", "srv", Set.of(), 0, Duration.ofDays(7))));
    }

    @Test
    void pruneByOrigin() throws IOException {
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        Backup manual = create(profile, BackupOrigin.MANUAL);
        Backup automatic = create(profile, BackupOrigin.LAUNCH);

        assertEquals(List.of(automatic), service.prune(new PruneRequest(null, null, BackupOrigin.AUTOMATIC, 0, null)));
        assertEquals(List.of(manual), service.list());
    }

    @Test
    void pruneRequiresKeepOrAge() {
        assertThrows(IllegalArgumentException.class, () -> new PruneRequest(null, null, Set.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> new PruneRequest(null, null, Set.of(), -1, null));
    }

    @Test
    void automaticBackupsArePrunedAutomatically() throws IOException {
        env.getConfig().setAutoKeep(2);
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        Backup manual = create(profile, BackupOrigin.MANUAL);
        Backup first = create(profile, BackupOrigin.LAUNCH);
        env.getClock().advance(Duration.ofSeconds(1));
        Backup second = create(profile, BackupOrigin.LAUNCH);
        env.getClock().advance(Duration.ofSeconds(1));
        Backup third = create(profile, BackupOrigin.LAUNCH);
        Backup secondManual = create(profile, BackupOrigin.MANUAL);

        List<Backup> backups = service.list();
        assertFalse(backups.contains(first));
        assertTrue(backups.containsAll(List.of(manual, second, third, secondManual)));
    }

    @Test
    void automaticPruningCanBeDisabled() throws IOException {
        env.getConfig().setAutoKeep(0);
        Profile profile = server();
        write(profile.path().resolve("server.jar"), "jar");
        for (int i = 0; i < 3; i++) {
            create(profile, BackupOrigin.LAUNCH);
        }

        assertEquals(3, service.list().size());
    }

    @Test
    void otherProvidersCanBeBackedUp() throws IOException {
        write(root.resolve("container").resolve("data").resolve("file.txt"), "data");

        Backup backup = service.create(new BackupRequest(
            "directory", root.resolve("container").toString(), null, null, null, List.of(), true, BackupOrigin.MANUAL, false
        ));

        assertEquals("directory", backup.provider());
        assertNull(backup.profile());
        assertEquals(List.of("data/file.txt"), entries(backup));
        Files.delete(root.resolve("container").resolve("data").resolve("file.txt"));
        assertEquals(List.of("data/file.txt"), service.diff(backup, true).removed());

        service.restore(backup, RestoreOptions.DEFAULT);
        assertEquals("data", Files.readString(root.resolve("container").resolve("data").resolve("file.txt")));
    }

    /**
     * Backs up any directory, to test that the service does not depend on profiles.
     */
    private static class DirectoryProvider implements BackupProvider {
        @Override
        public String getName() {
            return "directory";
        }

        @Override
        public String getDescription() {
            return "Directories";
        }

        @Override
        public List<String> getTypes() {
            return List.of("full");
        }

        @Override
        public List<String> getTargets() {
            return List.of();
        }

        @Override
        public BackupPlan plan(BackupRequest request) {
            return new BackupPlan(request.target(), "full", null, Path.of(request.target()), List.of(""), List.of());
        }

        @Override
        public Optional<String> getUsage(BackupScope scope, @Nullable Profile profile) {
            return Optional.empty();
        }

    }

}
