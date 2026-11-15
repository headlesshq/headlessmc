package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Profiles written by older versions of HeadlessMc, in the formats 0 and 1, have to stay readable.
 * These formats stored a {@code version} and a {@code currentVersion} in the profile,
 * which have been replaced by the side and the launch target of the profile in format 2.
 */
class ProfileMigrationTest {
    private static final String[] LEGACY = {
        "v0-client", "v0-server", "v0-explicit-client", "v1-client", "v1-client-latest", "v1-server", "v1-full"
    };

    @TempDir
    Path root;

    private ProfileTestFiles files;
    private LaunchTargetService launchTargetService;

    @BeforeEach
    void setup() {
        files = new ProfileTestFiles(root);
        FakePlatformService platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1", "1.20.4"),
            FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.10", "0.16.9"}),
            FakePlatform.create("paper", new String[]{"1.21.1", "130"})
        );

        launchTargetService = new TestLaunchTargets(files.service)
            .with(
                new FakeVersionLaunchTarget(platformService, files.jsonService, Side.CLIENT, root),
                new FakeVersionLaunchTarget(platformService, files.jsonService, Side.SERVER, root)
            ).service();
    }

    private VersionHandle handle(Profile profile) {
        return files.jsonService.convert(profile.target().data(), VersionHandle.class);
    }

    private static VersionArg arg(String... args) {
        return VersionArg.parse(args);
    }

    @Test
    void format0ClientBecomesAVersionTarget() {
        Profile profile = files.load("v0-client");

        assertEquals("v0-client", profile.name());
        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertEquals(VersionHandle.VERSION, profile.target().name());
        // the version it had, not resolved yet
        assertEquals(new VersionHandle(arg("fabric", "1.21.1"), arg("fabric", "1.21.1"), null), handle(profile));
        // format 0 wrote the path as a file: URI
        assertEquals(files.gameDir, profile.path());
        assertEquals(List.of("-Xmx6G"), profile.vmArgs());
        assertEquals(List.of(), profile.gameArgs());
        assertEquals(List.of(), profile.patchers());
        assertEquals(Map.of(), profile.systemProperties());
        assertNull(profile.javaVersion());
        assertEquals(EulaStatus.UNKNOWN, profile.eulaStatus());
        assertTrue(profile.hasDefaultClientJvmArgs());
        assertTrue(profile.options().isEmpty());
        // the format is only updated when the profile is saved
        assertEquals(0, profile.hmcVersion());
    }

    @Test
    void format0ServerBecomesAServerTarget() {
        Profile profile = files.load("v0-server");

        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertTrue(profile.isServer());
        assertEquals(VersionHandle.SERVER, profile.target().name());
        VersionArg version = arg("server", "paper", "1.21.1");
        assertEquals(new VersionHandle(version, version, null), handle(profile));
        assertEquals(List.of("nogui"), profile.gameArgs());
        assertEquals(21, profile.javaVersion());
        assertEquals(EulaStatus.EXISTS, profile.eulaStatus());
    }

    @Test
    void aProfileWithoutASideHasAlwaysBeenAClient() {
        // this is the only place where a missing side means client
        assertEquals(Optional.of(Side.CLIENT), files.load("v0-client").side());
        assertEquals(Optional.of(Side.CLIENT), files.load("v1-client").side());
        assertTrue(files.load("v1-full").isClient());
    }

    @Test
    void anExplicitClientSideIsNotKeptInTheVersion() {
        Profile profile = files.load("v0-explicit-client");

        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertEquals(VersionHandle.VERSION, profile.target().name());
        // the side is a property of the profile, the version of the version target has none
        assertEquals(Optional.empty(), handle(profile).version().side());
        assertEquals(arg("1.21.1"), handle(profile).version());
    }

    @Test
    void format1ReadsRelativePaths() {
        assertEquals(files.mcFiles.getMcDir().resolve("fabric-1.21.1"), files.load("v1-client").path());
        assertEquals(
            files.appFiles.getDataDir().resolve("server").resolve("v1-server"),
            files.load("v1-server").path()
        );
    }

    @Test
    void format1PathFollowsAMovedMinecraftDirectory() {
        // the profile has been written when the .minecraft directory was somewhere else
        files.install("v1-client", root.resolve("old-mc"));

        Profile profile = files.service.getProfile("v1-client").orElseThrow();

        assertEquals(files.mcFiles.getMcDir().resolve("fabric-1.21.1"), profile.path());
    }

    @Test
    void specifiedAndCurrentVersionAreKeptApart() {
        Profile profile = files.load("v1-client-latest");

        assertEquals(arg("fabric", "1.21.1", "latest"), handle(profile).version());
        assertEquals(arg("fabric", "1.21.1", "0.16.9"), handle(profile).current());
        assertNull(handle(profile).currentTarget());
    }

    @Test
    void format1ServerKeepsItsSettings() {
        Profile profile = files.load("v1-server");

        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertEquals(VersionHandle.SERVER, profile.target().name());
        assertEquals(arg("server", "1.21.1"), handle(profile).version());
        assertEquals(EulaStatus.NONE, profile.eulaStatus());
        assertEquals(1, profile.hmcVersion());
    }

    @Test
    void everyFieldOfAProfileIsMigrated() {
        Profile profile = files.load("v1-full");

        VersionArg version = arg("fabric", "1.21.1", "0.16.9");
        assertEquals(new VersionHandle(version, version, null), handle(profile));
        assertEquals(Optional.of(new LaunchOptions.Resolution(1280, 720)), profile.options().resolution());
        assertEquals(Optional.of("quick.json"), profile.options().quickPlayPath());
        assertEquals(
            Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, "example.org:25566")),
            profile.options().join()
        );
        assertTrue(profile.options().demo());
        assertEquals(List.of("lwjgl", "log4j"), profile.patchers());
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("a", "b");
        properties.put("flag", null);
        assertEquals(properties, profile.systemProperties());
        assertEquals(List.of("-Xmx2G", "-Xms1G"), profile.vmArgs());
        assertEquals(List.of("--fullscreen"), profile.gameArgs());
        assertEquals(17, profile.javaVersion());
        assertTrue(profile.hasDefaultClientJvmArgs());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "v0-client", "v0-server", "v0-explicit-client", "v1-client", "v1-client-latest", "v1-server", "v1-full"
    })
    void readingDoesNotRewriteTheFile(String name) throws IOException {
        Path file = files.install(name);
        byte[] before = Files.readAllBytes(file);

        assertTrue(files.service.getProfile(name).isPresent());
        assertEquals(1, files.service.getProfiles().size());

        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "v0-client", "v0-server", "v0-explicit-client", "v1-client", "v1-client-latest", "v1-server", "v1-full"
    })
    void savingWritesTheCurrentFormat(String name) throws IOException {
        Profile migrated = files.load(name);

        Profile saved = files.service.save(migrated);

        assertEquals(Profile.PROFILE_VERSION, saved.hmcVersion());
        String json = Files.readString(files.profilesDir.resolve(name + ".json"));
        Set<String> keys = topLevelKeys(json);
        assertTrue(keys.containsAll(Set.of("name", "side", "target", "path", "hmcVersion")), json);
        assertFalse(keys.contains("version"), json);
        assertFalse(keys.contains("currentVersion"), json);
        assertTrue(json.contains("\"hmcVersion\" : " + Profile.PROFILE_VERSION), json);

        // and it is read back as the same profile
        Profile loaded = files.service.getProfile(name).orElseThrow();
        assertEquals(Profile.PROFILE_VERSION, loaded.hmcVersion());
        assertEquals(migrated.side(), loaded.side());
        assertEquals(migrated.target().name(), loaded.target().name());
        assertEquals(handle(migrated), handle(loaded));
        assertEquals(
            migrated.withHmcVersion(Profile.PROFILE_VERSION).withTarget(loaded.target()),
            loaded
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "v0-client", "v0-server", "v0-explicit-client", "v1-client", "v1-client-latest", "v1-server", "v1-full"
    })
    void migratedProfilesAreUnderstoodByTheLaunchTargetsForVersions(String name) {
        Profile profile = files.load(name);

        TargetInfo info = launchTargetService.describe(profile);
        assertEquals(Optional.of(handle(profile).version()), info.specified());
        assertEquals(Optional.ofNullable(handle(profile).current()), info.current());
        // not pinned to a file yet, that happens when the profile is launched for the first time
        assertEquals(Optional.empty(), info.file());
        assertEquals(handle(profile).version().version(), launchTargetService.requireVersion(profile).version());
        // resolving validates the side of the profile against its target
        assertSame(profile, launchTargetService.resolve(profile, new io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl()));
    }

    @Test
    void aMigratedProfileIsSavedWhenItIsResolvedForTheFirstTime() throws IOException {
        Path file = files.install("v1-client-latest");
        Profile profile = files.service.getProfile("v1-client-latest").orElseThrow();
        Path pin = files.mcFiles.getVersionsDir().resolve("fabric").resolve("fabric.json");
        // a launch target that pins the version it resolved
        FakeVersionLaunchTarget pinning = new FakeVersionLaunchTarget(
            new FakePlatformService(new FakeVanillaPlatform("1.21.1")), files.jsonService, Side.CLIENT, root
        ) {
            @Override
            public Profile resolve(Profile toResolve, io.github.headlesshq.headlessmc.util.typemap.TypedMap options) {
                VersionHandle handle = handle(toResolve);
                return handle.currentTarget() != null
                    ? toResolve
                    : toResolve.withTarget(handle(handle.withCurrentTarget(pin)));
            }
        };

        LaunchTargetService service = new TestLaunchTargets(files.service).with(pinning).service();
        Profile resolved = service.resolveAndPersist(profile, new io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl());

        assertEquals(pin, handle(resolved).currentTarget());
        assertTrue(Files.readString(file).contains("\"hmcVersion\" : " + Profile.PROFILE_VERSION));
        Profile stored = files.service.getProfile("v1-client-latest").orElseThrow();
        assertEquals(pin, handle(stored).currentTarget());
        // the second time nothing changes and nothing is written
        byte[] bytes = Files.readAllBytes(file);
        assertSame(stored, service.resolveAndPersist(stored, new io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl()));
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }

    @Test
    void profilesOfAllFormatsAreListedTogether() {
        for (String name : LEGACY) {
            files.install(name);
        }

        files.install("v2-version");
        files.install("v2-server");
        files.install("v3-future");
        files.install("garbage");
        files.install("v1-missing-version");
        files.install("v2-invalid-missing-target");

        Set<String> names = files.service.getProfiles().stream().map(Profile::name).collect(Collectors.toSet());

        Set<String> expected = new java.util.HashSet<>(List.of(LEGACY));
        expected.add("v2-version");
        expected.add("v2-server");
        assertEquals(expected, names);
    }

    @Test
    void profilesOfANewerFormatAreSkippedAndLeftAlone() throws IOException {
        Path file = files.install("v3-future");
        byte[] before = Files.readAllBytes(file);

        assertEquals(Optional.empty(), files.service.getProfile("v3-future"));
        assertEquals(List.of(), files.service.getProfiles());
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void aLegacyProfileWithoutVersionIsSkipped() {
        files.install("v1-missing-version");
        files.install("v1-client");

        assertEquals(Optional.empty(), files.service.getProfile("v1-missing-version"));
        assertEquals(List.of("v1-client"), files.service.getProfiles().stream().map(Profile::name).toList());
    }

    /** The keys of the profile object in a pretty printed profile file, they are indented by two spaces. */
    static Set<String> topLevelKeys(String json) {
        return json.lines()
            .filter(line -> line.startsWith("  \""))
            .map(line -> line.substring(3, line.indexOf('"', 3)))
            .collect(Collectors.toSet());
    }

}
