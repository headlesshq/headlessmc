package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.LATEST_BUILD;
import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.OLD_BUILD;
import static io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget.MC_FILES;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Removing versions with multiple {@code .minecraft} directories, and with profiles pinned to a version.json.
 * The same version, fabric 1.21.1 0.16.9, is installed in both directories.
 */
class VersionRemoveServiceMultiDirTest {
    private static final String FABRIC_OLD = "fabric-loader-" + OLD_BUILD + "-1.21.1";
    private static final String FABRIC_LATEST = "fabric-loader-" + LATEST_BUILD + "-1.21.1";

    @TempDir
    Path root;

    private FakeProfileService profileService;
    private ClientTargetEnvironment env;
    private VersionRemoveService service;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root.resolve("profiles"));
        env = new ClientTargetEnvironment(root, profileService);
        service = new VersionRemoveService(
            env.matcherService, env.versionJsonService, env.platformService, profileService,
            env.fileService, env.jsonService, env.launchTargetService
        );
        env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        env.install(env.secondary, "fabric", "1.21.1", OLD_BUILD);
    }

    private Version version(McFiles mcFiles, String name) {
        return env.versionJsonService.tryGetVersion(mcFiles, name).orElseThrow();
    }

    private static List<String> names(List<Profile> profiles) {
        return profiles.stream().map(Profile::name).toList();
    }

    private static List<String> ids(List<Version> versions) {
        return versions.stream().map(Version::getId).toList();
    }

    /** Saves a profile for fabric 1.21.1 that has been resolved in the given directory. */
    private Profile pinned(String name, McFiles mcFiles) {
        TypedMap options = new TypedMapImpl();
        options.put(MC_FILES, mcFiles);
        Profile profile = new Profile(name, VersionArg.parse("fabric", "1.21.1"), root.resolve(name));
        return profileService.save(env.version.resolve(profile, options));
    }

    // finding the version

    @Test
    void aVersionInstalledInMultipleDirectoriesNeedsTheDirectory() {
        for (List<String> args : List.of(List.of(FABRIC_OLD), List.of("fabric", "1.21.1", OLD_BUILD), List.of("fabric", "1.21.1"))) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.findVersion(args));

            assertTrue(e.getMessage().contains("is installed in multiple Minecraft directories"), e.getMessage());
            assertTrue(e.getMessage().contains(env.primary.getMcDir().toString()), e.getMessage());
            assertTrue(e.getMessage().contains(env.secondary.getMcDir().toString()), e.getMessage());
            assertTrue(e.getMessage().contains("--mc <path>"), e.getMessage());
        }
    }

    @Test
    void theDirectorySelectsTheInstallation() {
        Version inPrimary = service.findVersion(List.of("fabric", "1.21.1"), env.primary);
        Version inSecondary = service.findVersion(List.of(FABRIC_OLD), env.secondary);

        assertEquals(env.json(env.primary, FABRIC_OLD), inPrimary.asFile().orElseThrow());
        assertEquals(env.json(env.secondary, FABRIC_OLD), inSecondary.asFile().orElseThrow());
    }

    @Test
    void aVersionInstalledInOneDirectoryIsFoundWithoutTheDirectory() {
        env.install(env.secondary, "fabric", "1.21.1", LATEST_BUILD);

        Version version = service.findVersion(List.of("fabric", "1.21.1", LATEST_BUILD));

        assertEquals(env.json(env.secondary, FABRIC_LATEST), version.asFile().orElseThrow());
        // but it is not in the other directory
        assertThrows(
            IllegalArgumentException.class, () -> service.findVersion(List.of("fabric", "1.21.1", LATEST_BUILD), env.primary)
        );
    }

    @Test
    void differentBuildsAreStillToldApartByTheirBuild() {
        env.install(env.primary, "fabric", "1.21.1", LATEST_BUILD);

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> service.findVersion(List.of("fabric", "1.21.1"), env.primary)
        );

        assertTrue(e.getMessage().contains("please specify the build"), e.getMessage());
    }

    // what is removed

    @Test
    void onlyTheDirectoryOfTheVersionIsChanged() {
        VersionRemoval removal = service.plan(version(env.secondary, FABRIC_OLD));

        assertEquals(env.secondary, removal.mcFiles());
        assertEquals(List.of(env.secondary.getVersionsDir().resolve(FABRIC_OLD)), removal.files());

        service.remove(removal);

        assertFalse(Files.exists(env.json(env.secondary, FABRIC_OLD)));
        assertTrue(Files.exists(env.json(env.secondary, "1.21.1")));
        assertTrue(Files.exists(env.json(env.primary, FABRIC_OLD)));
        assertTrue(Files.exists(env.json(env.primary, "1.21.1")));
    }

    @Test
    void onlyVersionsInTheSameDirectoryDependOnAVersion() {
        env.install(env.primary, "fabric", "1.21.1", LATEST_BUILD);

        VersionRemoval inSecondary = service.plan(version(env.secondary, "1.21.1"), true);
        VersionRemoval inPrimary = service.plan(version(env.primary, "1.21.1"));

        // the fabric versions of the other directory inherit from the 1.21.1 of their own directory
        assertEquals(List.of(FABRIC_OLD), ids(inSecondary.dependentVersions()));
        assertEquals(List.of("1.21.1", FABRIC_OLD), ids(inSecondary.removedVersions()));
        assertTrue(inSecondary.files().stream().allMatch(file -> file.startsWith(env.secondary.getMcDir())));
        assertEquals(List.of(FABRIC_LATEST, FABRIC_OLD), ids(inPrimary.dependentVersions()).stream().sorted().toList());
        assertTrue(inPrimary.isBlocked());
    }

    // profiles

    @Test
    void aProfileBlocksTheRemovalOfTheFileItIsPinnedTo() {
        pinned("on-ssd", env.secondary);

        VersionRemoval pinnedFile = service.plan(version(env.secondary, FABRIC_OLD));
        VersionRemoval otherFile = service.plan(version(env.primary, FABRIC_OLD));

        // the other directory has the same version, but the profile launches this file
        assertEquals(List.of("on-ssd"), names(pinnedFile.blockingProfiles()));
        assertTrue(pinnedFile.isBlocked());
        assertEquals(List.of(), names(otherFile.blockingProfiles()));
        assertFalse(otherFile.isBlocked());
    }

    @Test
    void aProfileThatIsNotPinnedIsLaunchedFromAnyDirectory() {
        profileService.save(new Profile("anywhere", VersionArg.parse("fabric", "1.21.1"), root));

        VersionRemoval first = service.plan(version(env.primary, FABRIC_OLD));
        assertEquals(List.of(), names(first.blockingProfiles()));
        service.remove(first);

        // now it is the last installation that can launch the profile
        VersionRemoval last = service.plan(version(env.secondary, FABRIC_OLD));
        assertEquals(List.of("anywhere"), names(last.blockingProfiles()));
    }

    @Test
    void aProfileWhosePinIsBrokenIsTreatedLikeAProfileThatIsNotPinned() throws IOException {
        pinned("broken", env.primary);
        Files.delete(env.json(env.primary, FABRIC_OLD));
        env.install(env.primary, "fabric", "1.21.1", LATEST_BUILD);

        // the next launch resolves it again, to the build it had: only the other directory has that
        assertEquals(List.of("broken"), names(service.plan(version(env.secondary, FABRIC_OLD)).blockingProfiles()));
        assertEquals(List.of(), names(service.plan(version(env.primary, FABRIC_LATEST)).blockingProfiles()));
    }

    @Test
    void profilesPinnedToDependentVersionsBlockAndAreRemovedWithThem() {
        pinned("on-ssd", env.secondary);
        pinned("on-primary", env.primary);

        VersionRemoval blocked = service.plan(version(env.secondary, "1.21.1"));
        VersionRemoval withDependents = service.plan(version(env.secondary, "1.21.1"), true);

        assertEquals(List.of("on-ssd"), names(blocked.blockingProfiles()));
        assertEquals(List.of(), names(blocked.removedProfiles()));
        assertEquals(List.of("on-ssd"), names(withDependents.removedProfiles()));
        assertFalse(withDependents.isBlocked());

        service.remove(withDependents);

        assertEquals(List.of("on-primary"), names(profileService.getProfiles()));
        assertFalse(Files.exists(env.secondary.getVersionsDir().resolve("1.21.1")));
        assertTrue(Files.exists(env.json(env.primary, FABRIC_OLD)));
    }

    @Test
    void aProfileForAVersionJsonBlocksTheRemovalOfThatFile() {
        Path json = env.installCustom(env.secondary, "my-pack");
        profileService.save(env.versionFile.create(
            "file", env.versionFile.parse(TargetArgs.of(json.toString())), new TypedMapImpl()
        ));

        assertEquals(List.of("file"), names(service.plan(version(env.secondary, "my-pack")).blockingProfiles()));
        assertEquals(List.of(), names(service.plan(version(env.secondary, FABRIC_OLD)).blockingProfiles()));
    }

    @Test
    void aPinnedProfileOfAnUnavailablePlatformStillBlocks() {
        pinned("on-ssd", env.secondary);
        env.removeFabric();

        assertEquals(List.of("on-ssd"), names(service.plan(version(env.secondary, FABRIC_OLD)).blockingProfiles()));
        assertEquals(List.of(), names(service.plan(version(env.primary, FABRIC_OLD)).blockingProfiles()));
    }

    // version directories that are also game directories, see GameDirLayout

    @Test
    void theFilesOfAGamePlayedInTheVersionDirectoryAreNeverDeleted() throws IOException {
        // like HMCL and PCL2 do: worlds and mods next to the version.json
        Path versionDir = env.secondary.getVersionsDir().resolve(FABRIC_OLD);
        Files.writeString(versionDir.resolve(FABRIC_OLD + ".jar"), "jar");
        Files.createDirectories(versionDir.resolve("natives-linux"));
        Path level = Files.createDirectories(versionDir.resolve("saves").resolve("world")).resolve("level.dat");
        Files.writeString(level, "level");
        Files.writeString(versionDir.resolve("options.txt"), "fov:70");

        VersionRemoval removal = service.plan(version(env.secondary, FABRIC_OLD));

        // only the files of the version
        assertEquals(List.of(versionDir), removal.keptDirectories());
        assertEquals(
            List.of(versionDir.resolve(FABRIC_OLD + ".jar"), versionDir.resolve(FABRIC_OLD + ".json"), versionDir.resolve("natives-linux")),
            removal.files()
        );

        service.remove(removal);

        assertFalse(Files.exists(env.json(env.secondary, FABRIC_OLD)));
        assertFalse(Files.exists(versionDir.resolve(FABRIC_OLD + ".jar")));
        assertFalse(Files.exists(versionDir.resolve("natives-linux")));
        assertEquals("level", Files.readString(level));
        assertEquals("fov:70", Files.readString(versionDir.resolve("options.txt")));
        // the version is gone for HeadlessMc
        assertTrue(env.versionJsonService.tryGetVersion(env.secondary, FABRIC_OLD).isEmpty());
    }

    @Test
    void aVersionDirectoryThatOnlyHoldsTheVersionIsDeletedAsAWhole() throws IOException {
        Path versionDir = env.secondary.getVersionsDir().resolve(FABRIC_OLD);
        Files.writeString(versionDir.resolve(FABRIC_OLD + ".jar"), "jar");
        Files.createDirectories(versionDir.resolve(FABRIC_OLD + "-natives"));

        VersionRemoval removal = service.plan(version(env.secondary, FABRIC_OLD));
        service.remove(removal);

        assertEquals(List.of(), removal.keptDirectories());
        assertEquals(List.of(versionDir), removal.files());
        assertFalse(Files.exists(versionDir));
    }

    @Test
    void gamesInTheDirectoriesOfDependentVersionsAreKeptAsWell() throws IOException {
        Path fabricDir = env.secondary.getVersionsDir().resolve(FABRIC_OLD);
        Files.writeString(fabricDir.resolve("options.txt"), "fov:70");

        VersionRemoval removal = service.plan(version(env.secondary, "1.21.1"), true);
        service.remove(removal);

        assertEquals(List.of(fabricDir), removal.keptDirectories());
        assertEquals("fov:70", Files.readString(fabricDir.resolve("options.txt")));
        assertFalse(Files.exists(env.json(env.secondary, FABRIC_OLD)));
        // nothing is played in the directory of the vanilla version
        assertFalse(Files.exists(env.secondary.getVersionsDir().resolve("1.21.1")));
    }

}
