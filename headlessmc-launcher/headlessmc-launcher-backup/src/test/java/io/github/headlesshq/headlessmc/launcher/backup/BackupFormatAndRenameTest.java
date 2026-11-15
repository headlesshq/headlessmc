package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.version.arg.Side;
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

/**
 * Backups contain the profile they have been created from.
 * Backups written before profiles had launch targets (format 0) have to stay readable and restorable,
 * and backups have to follow their profile when it is renamed.
 */
class BackupFormatAndRenameTest {
    /** A profile as it has been written into backups of format 0, with a version instead of a launch target. */
    private static final String LEGACY_PROFILE = """
        {
            "name" : "%s",
            "version" : { "side" : %s, "platform" : "%s", "version" : "1.21.1", "build" : null },
            "currentVersion" : { "side" : %s, "platform" : "%s", "version" : "1.21.1", "build" : "0.16.9" },
            "path" : "%s",
            "options" : { "resolution" : null, "quickPlayPath" : null, "join" : null, "demo" : false },
            "patchers" : [ ],
            "systemProperties" : { },
            "vmArgs" : [ "-Xmx4G" ],
            "gameArgs" : [ ],
            "javaVersion" : 21,
            "eulaStatus" : "UNKNOWN",
            "hasDefaultClientJvmArgs" : false,
            "hmcVersion" : 1
          }""";

    @TempDir
    Path root;

    private TestBackupEnvironment env;
    private BackupServiceImpl service;

    @BeforeEach
    void setup() {
        env = new TestBackupEnvironment(root);
        service = env.getBackupService();
    }

    private Profile client() {
        return env.getProfileService().save(new Profile("Main", VersionArg.parse("fabric", "1.21.1"), root.resolve("main")));
    }

    private Profile server() {
        return env.getProfileService().save(new Profile("srv", VersionArg.parse("server", "fabric", "1.21.1"), root.resolve("srv")));
    }

    private Backup create(Profile profile) throws IOException {
        Files.createDirectories(profile.path());
        Files.writeString(profile.path().resolve("options.txt"), "fov:70");
        Backup backup = service.create(
            new BackupRequest("profile", profile.name(), null, null, null, List.of(), true, BackupOrigin.MANUAL, false)
        );
        env.getClock().advance(Duration.ofSeconds(1));
        return backup;
    }

    private Path json(Backup backup) {
        return service.getArchive(backup).resolveSibling(backup.id() + ".json");
    }

    private Path targetDir(String target) {
        return service.getBackupDir().resolve("profile").resolve(target);
    }

    /**
     * Rewrites the json of a backup into format 0: the profile in the old format, and the old format version.
     *
     * @return the content of the rewritten file.
     */
    private String toFormat0(Backup backup) throws IOException {
        Profile profile = backup.profile();
        assertNotNull(profile);
        String side = profile.isServer() ? "\"SERVER\"" : "null";
        String legacyProfile = LEGACY_PROFILE.formatted(
            profile.name(), side, "fabric", side, "fabric", profile.path().toUri()
        );

        String text = Files.readString(json(backup));
        int key = text.indexOf("\"profile\"");
        int start = text.indexOf('{', key);
        int end = start;
        for (int depth = 0; end < text.length(); end++) {
            if (text.charAt(end) == '{') {
                depth++;
            } else if (text.charAt(end) == '}' && --depth == 0) {
                break;
            }
        }

        String legacy = text.substring(0, start) + legacyProfile + text.substring(end + 1);
        int version = legacy.lastIndexOf("\"hmcVersion\"");
        legacy = legacy.substring(0, version) + legacy.substring(version).replaceFirst("\\d+", "0");
        assertNotEquals(text, legacy);
        Files.writeString(json(backup), legacy);
        return legacy;
    }

    // the format

    @Test
    void newBackupsAreWrittenInTheCurrentFormat() throws IOException {
        Backup backup = create(client());

        assertEquals(1, Backup.BACKUP_VERSION);
        assertEquals(Backup.BACKUP_VERSION, backup.hmcVersion());
        String text = Files.readString(json(backup));
        assertTrue(text.contains("\"target\""), text);
        assertFalse(text.contains("\"currentVersion\""), text);
    }

    @Test
    void aClientBackupOfFormat0IsRead() throws IOException {
        Backup backup = create(client());
        String legacy = toFormat0(backup);

        Backup read = service.get(backup.id()).orElseThrow();

        // everything about the backup itself is the same
        assertEquals(backup.id(), read.id());
        assertEquals(backup.target(), read.target());
        assertEquals(backup.source(), read.source());
        assertEquals(backup.created(), read.created());
        assertEquals(backup.files(), read.files());
        assertEquals(0, read.hmcVersion());
        // its profile has been converted to a profile with a launch target
        Profile profile = read.profile();
        assertNotNull(profile);
        assertEquals("Main", profile.name());
        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertEquals(VersionHandle.VERSION, profile.target().name());
        assertEquals(backup.profile().path(), profile.path());
        assertEquals(List.of("-Xmx4G"), profile.vmArgs());
        assertEquals(21, profile.javaVersion());
        // the version, and what it had been resolved to, are in the target
        VersionHandle handle = (VersionHandle) profile.target().data();
        assertEquals(VersionArg.parse("fabric", "1.21.1"), handle.version());
        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.9"), handle.current());
        assertNull(handle.currentTarget());
        // reading a backup never rewrites it
        assertEquals(legacy, Files.readString(json(backup)));
        assertEquals(List.of(read), service.list());
    }

    @Test
    void aServerBackupOfFormat0IsRead() throws IOException {
        Backup backup = create(server());
        toFormat0(backup);

        Profile profile = service.get(backup.id()).orElseThrow().profile();

        assertNotNull(profile);
        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertEquals(VersionHandle.SERVER, profile.target().name());
        assertEquals(VersionArg.parse("server", "fabric", "1.21.1"), ((VersionHandle) profile.target().data()).version());
    }

    @Test
    void aBackupOfFormat0CanBeComparedAndRestored() throws IOException {
        Profile profile = server();
        Backup backup = create(profile);
        toFormat0(backup);
        Backup legacy = service.get(backup.id()).orElseThrow();
        Files.writeString(profile.path().resolve("options.txt"), "fov:110");
        // the profile has been removed in the meantime
        env.getProfileService().remove(profile);

        assertEquals(List.of("options.txt"), service.diff(legacy, true).modified());
        service.restore(legacy, new RestoreOptions(false, false, false));

        assertEquals("fov:70", Files.readString(profile.path().resolve("options.txt")));
        // the profile comes back in the current format
        Profile restored = env.getProfileService().getProfile("srv").orElseThrow();
        assertEquals(Optional.of(Side.SERVER), restored.side());
        assertEquals(VersionHandle.SERVER, restored.target().name());
        assertEquals(List.of("-Xmx4G"), restored.vmArgs());
    }

    @Test
    void backupsOfANewerFormatAreIgnored() throws IOException {
        Backup backup = create(client());
        String text = Files.readString(json(backup));
        int version = text.lastIndexOf("\"hmcVersion\"");
        Files.writeString(json(backup), text.substring(0, version) + text.substring(version).replaceFirst("\\d+", "2"));

        assertEquals(List.of(), service.list());
        assertEquals(Optional.empty(), service.get(backup.id()));
        // it is not touched, a newer HeadlessMc can still read it
        assertTrue(Files.exists(json(backup)));
        assertTrue(Files.exists(service.getArchive(backup)));
    }

    @Test
    void aBackupThatCannotBeReadDoesNotHideTheOthers() throws IOException {
        Backup broken = create(client());
        Backup good = create(server());
        Files.writeString(json(broken), "{ not json");

        assertEquals(List.of(good), service.list());
    }

    // renaming

    @Test
    void backupsAreMovedToTheirNewTarget() throws IOException {
        Profile profile = server();
        Backup first = create(profile);
        Backup second = create(profile);
        Backup other = create(client());

        List<Backup> renamed = service.rename("profile", "srv", "survival");

        assertEquals(List.of(first.id(), second.id()), renamed.stream().map(Backup::id).toList());
        for (Backup backup : renamed) {
            assertEquals("survival", backup.target());
            // also the profile inside, a restore must not bring back a profile with the old name
            assertEquals("survival", backup.profile().name());
            assertEquals(profile.path(), backup.source());
            assertEquals(targetDir("survival").resolve(backup.id() + ".zip"), service.getArchive(backup));
            assertTrue(Files.exists(service.getArchive(backup)));
            assertTrue(Files.exists(json(backup)));
        }

        // nothing is left behind
        assertFalse(Files.exists(targetDir("srv")));
        assertEquals(List.of(), service.list("profile", "srv"));
        assertEquals(renamed, service.list("profile", "survival"));
        assertEquals(List.of(other), service.list("profile", "Main"));
        // the files of the backups are untouched
        assertEquals(first.files(), renamed.getFirst().files());
        assertTrue(service.diff(renamed.getFirst(), true).isEmpty());
    }

    @Test
    void renamedBackupsCanBeRestored() throws IOException {
        Profile profile = server();
        create(profile);
        Backup renamed = service.rename("profile", "srv", "survival").getFirst();
        Files.writeString(profile.path().resolve("options.txt"), "fov:110");

        service.restore(renamed, new RestoreOptions(false, false, false));

        assertEquals("fov:70", Files.readString(profile.path().resolve("options.txt")));
        assertTrue(env.getProfileService().getProfile("survival").isPresent());
    }

    @Test
    void newBackupsOfARenamedProfileJoinItsOldOnes() throws IOException {
        Profile profile = server();
        Backup old = create(profile);
        service.rename("profile", "srv", "survival");
        env.getProfileService().remove(profile);
        Profile renamed = env.getProfileService().save(profile.withName("survival"));

        Backup created = create(renamed);

        assertEquals(
            List.of(old.id(), created.id()), service.list("profile", "survival").stream().map(Backup::id).toList()
        );
        assertNotEquals(old.id(), created.id());
    }

    @Test
    void renamingToANameThatOnlyDiffersInCaseKeepsTheFiles() throws IOException {
        Backup backup = create(server());

        Backup renamed = service.rename("profile", "srv", "SRV").getFirst();

        // backups are stored under the lowercase name
        assertEquals("SRV", renamed.target());
        assertEquals(service.getArchive(backup), service.getArchive(renamed));
        assertTrue(Files.exists(service.getArchive(renamed)));
        assertEquals(List.of(renamed), service.list());
    }

    @Test
    void renamingATargetWithoutBackupsDoesNothing() throws IOException {
        Backup backup = create(client());

        assertEquals(List.of(), service.rename("profile", "srv", "survival"));
        assertEquals(List.of(), service.rename("directory", "Main", "other"));

        assertEquals(List.of(backup), service.list());
        assertFalse(Files.exists(targetDir("survival")));
    }

    @Test
    void renamingABackupOfFormat0WritesItInTheCurrentFormat() throws IOException {
        Backup backup = create(server());
        toFormat0(backup);

        Backup renamed = service.rename("profile", "srv", "survival").getFirst();

        assertEquals(Backup.BACKUP_VERSION, renamed.hmcVersion());
        String text = Files.readString(json(renamed));
        assertFalse(text.contains("\"currentVersion\""), text);
        assertEquals("survival", renamed.profile().name());
        assertEquals(VersionHandle.SERVER, renamed.profile().target().name());
        // and it is read like any other backup
        assertEquals(List.of(renamed), service.list());
    }

}
