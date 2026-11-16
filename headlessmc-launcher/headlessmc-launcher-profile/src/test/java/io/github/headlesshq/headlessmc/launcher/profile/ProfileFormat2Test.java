package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The current profile format, with a side and a launch target.
 * Some things are legitimately missing in a stored profile, because they are only known once the profile
 * has been resolved. Everything else has to be there, a broken file is reported when it is read.
 */
class ProfileFormat2Test {
    @TempDir
    Path root;

    private ProfileTestFiles files;
    private TestLaunchTargets targets;
    private LaunchTargetService launchTargetService;

    @BeforeEach
    void setup() {
        files = new ProfileTestFiles(root);
        FakePlatformService platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1", "1.20.4"),
            FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.10", "0.16.9"}),
            FakePlatform.create("paper", new String[]{"1.21.1", "130"})
        );

        targets = new TestLaunchTargets(files.service)
            .with(
                new FakeVersionLaunchTarget(platformService, files.jsonService, Side.CLIENT, root),
                new FakeVersionLaunchTarget(platformService, files.jsonService, Side.SERVER, root)
            );
        targets.with(new ProfileLaunchTarget(targets.service(), files.service, files.jsonService));
        launchTargetService = targets.service();
    }

    private VersionHandle handle(Profile profile) {
        return files.jsonService.convert(profile.target().data(), VersionHandle.class);
    }

    @Test
    void readsAResolvedVersionProfile() {
        Profile profile = files.load("v2-version");

        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertEquals(VersionHandle.VERSION, profile.target().name());
        VersionHandle handle = handle(profile);
        assertEquals(VersionArg.parse("fabric", "1.21.1", "latest"), handle.version());
        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.9"), handle.current());
        Path pin = files.mcFiles.getVersionsDir()
            .resolve("fabric-loader-0.16.9-1.21.1")
            .resolve("fabric-loader-0.16.9-1.21.1.json");
        assertEquals(pin, handle.currentTarget());

        TargetInfo info = launchTargetService.describe(profile);
        assertEquals(Optional.of(handle.version()), info.specified());
        assertEquals(Optional.of(handle.current()), info.current());
        assertEquals(Optional.of(pin), info.file());
        assertEquals(handle.current(), launchTargetService.requireVersion(profile));
    }

    @Test
    void aProfileThatHasNeverBeenResolvedHasNoCurrentVersionAndNoPin() {
        Profile profile = files.load("v2-version-unresolved");

        VersionHandle handle = handle(profile);
        assertNull(handle.current());
        assertNull(handle.currentTarget());
        assertEquals(handle.version(), handle.effectiveVersion());

        TargetInfo info = launchTargetService.describe(profile);
        assertEquals(Optional.of(handle.version()), info.specified());
        assertEquals(Optional.empty(), info.current());
        assertEquals(Optional.empty(), info.file());
        // the specified version is the best known one
        assertEquals(Optional.of(handle.version()), info.version());
        assertTrue(info.isVersionKnown());
    }

    @Test
    void readsAServerProfile() {
        Profile profile = files.load("v2-server");

        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertEquals(VersionHandle.SERVER, profile.target().name());
        assertEquals(VersionArg.parse("server", "paper", "1.21.1", "130"), handle(profile).current());
        assertEquals(
            files.appFiles.getDataDir().resolve("server").resolve("v2-server").resolve("server.jar"),
            handle(profile).currentTarget()
        );
        assertEquals(21, profile.javaVersion());
    }

    @Test
    void readsAProfileLaunchingAnotherProfile() {
        files.install("v2-version");
        Profile profile = files.load("v2-profile");

        assertEquals(ProfileLaunchTarget.NAME, profile.target().name());
        assertEquals(
            new ProfileLaunchTarget.Handle("v2-version"),
            files.jsonService.convert(profile.target().data(), ProfileLaunchTarget.Handle.class)
        );
        // it describes itself as what its parent launches
        assertEquals(launchTargetService.describe(files.service.getProfile("v2-version").orElseThrow()),
                     launchTargetService.describe(profile));
        assertEquals("profile:v2-version", launchTargetService.getLabel(profile));
    }

    @ParameterizedTest
    @ValueSource(strings = {"v2-no-side", "v2-null-side", "v2-minimal"})
    void aMissingSideAndANullSideBothMeanNoSide(String name) {
        Profile profile = files.load(name);

        assertEquals(Optional.empty(), profile.side());
        // no side is a state of its own, it is neither of the two
        assertFalse(profile.isClient());
        assertFalse(profile.isServer());
    }

    @Test
    void everythingOptionalHasADefault() {
        Profile profile = files.load("v2-minimal");

        assertTrue(profile.options().isEmpty());
        assertEquals(List.of(), profile.patchers());
        assertEquals(Map.of(), profile.systemProperties());
        assertEquals(List.of(), profile.vmArgs());
        assertNull(profile.gameArgs());
        assertNull(profile.javaVersion());
        assertEquals(EulaStatus.UNKNOWN, profile.eulaStatus());
        assertFalse(profile.hasDefaultClientJvmArgs());
        assertEquals(files.appFiles.getDataDir().resolve("tools"), profile.path());
    }

    @Test
    void aProfileWithAnUnknownTargetIsReadListedAndSavedWithoutLoss() throws IOException {
        Path file = files.install("v2-unknown-target");
        Profile profile = files.service.getProfile("v2-unknown-target").orElseThrow();

        // the profile itself is fine, it even knows its side without its target
        assertEquals("from-a-plugin", profile.target().name());
        assertEquals(Optional.of(Side.CLIENT), profile.side());
        assertTrue(profile.target().data() instanceof Map<?, ?>);
        assertEquals(1, files.service.getProfiles().size());

        // nothing can be said about what it launches, but asking does not fail
        assertEquals(TargetInfo.EMPTY, launchTargetService.describe(profile));
        assertEquals("from-a-plugin", launchTargetService.getLabel(profile));
        assertThrows(IllegalArgumentException.class, () -> launchTargetService.requireVersion(profile));
        RuntimeException e = assertThrows(
            RuntimeException.class, () -> launchTargetService.resolve(profile, new TypedMapImpl())
        );
        assertTrue(e.getMessage().contains("from-a-plugin"), e.getMessage());
        assertTrue(e.getMessage().contains("not available"), e.getMessage());

        // saving it does not lose the data only the missing target understands
        files.service.save(profile);
        byte[] first = Files.readAllBytes(file);
        Profile loaded = files.service.getProfile("v2-unknown-target").orElseThrow();
        assertEquals(profile, loaded);
        files.service.save(loaded);
        assertArrayEquals(first, Files.readAllBytes(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "v2-invalid-missing-target", "v2-invalid-missing-target-name", "v2-invalid-missing-target-data",
        "v2-invalid-missing-path", "v2-invalid-missing-name", "garbage"
    })
    void aProfileMissingSomethingRequiredIsSkippedTheOthersAreStillRead(String name) {
        files.install(name);
        files.install("v2-version");

        assertEquals(Optional.empty(), files.service.getProfile(name));
        assertEquals(List.of("v2-version"), files.service.getProfiles().stream().map(Profile::name).toList());
    }

    @Test
    void theMissingFieldOfABrokenProfileIsNamed() {
        NullPointerException noTarget = assertThrows(NullPointerException.class, () -> new Profile(
            "broken", Optional.empty(), null, root, null, null, null, null, null, null, null, false, 2
        ));
        assertEquals("Profile broken is missing 'target'", noTarget.getMessage());

        NullPointerException noPath = assertThrows(NullPointerException.class, () -> new Profile(
            "broken", Optional.empty(), new LaunchTargetHandle("jar", Map.of()), null
        ));
        assertEquals("Profile broken is missing 'path'", noPath.getMessage());

        assertEquals("Launch target is missing 'name'",
                     assertThrows(NullPointerException.class, () -> new LaunchTargetHandle(null, Map.of())).getMessage());
        assertEquals("Launch target jar is missing 'data'",
                     assertThrows(NullPointerException.class, () -> new LaunchTargetHandle("jar", null)).getMessage());
        assertEquals("Launch target data is missing 'version'",
                     assertThrows(NullPointerException.class, () -> new VersionHandle(null, null, null)).getMessage());
    }

    @Test
    void invalidTargetDataIsReportedWhenTheTargetIsUsed() {
        Profile profile = files.load("v2-version")
            .withTarget(new LaunchTargetHandle(VersionHandle.VERSION, Map.of("currentTarget", "x")));

        // describing is used for listings, it must not fail
        assertEquals(TargetInfo.EMPTY, launchTargetService.describe(profile));
        RuntimeException e = assertThrows(
            RuntimeException.class, () -> launchTargetService.resolve(profile, new TypedMapImpl())
        );
        assertTrue(e.getMessage().contains("v2-version"), e.getMessage());
        assertTrue(e.getMessage().contains("missing 'version'"), e.getMessage());
    }

    @Test
    void aTargetThatDoesNotFitTheSideOfTheProfileIsRejected() {
        Profile client = files.load("v2-version");
        Profile sideless = client.withSide(Optional.empty());
        Profile server = client.withSide(Optional.of(Side.SERVER));

        assertSame(client, launchTargetService.resolve(client, new TypedMapImpl()));
        assertThrows(RuntimeException.class, () -> launchTargetService.resolve(sideless, new TypedMapImpl()));
        assertThrows(RuntimeException.class, () -> launchTargetService.resolve(server, new TypedMapImpl()));
    }

    @Test
    void aVersionWithTheWrongSideForItsTargetIsRejected() {
        Profile client = files.load("v2-version");
        VersionHandle serverVersion = VersionHandle.of(VersionArg.parse("server", "fabric", "1.21.1"));
        Profile broken = client.withTarget(new LaunchTargetHandle(VersionHandle.VERSION, serverVersion));

        RuntimeException e = assertThrows(
            RuntimeException.class, () -> launchTargetService.resolve(broken, new TypedMapImpl())
        );
        assertTrue(e.getMessage().contains("does not belong to launch target version"), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "v2-version", "v2-version-unresolved", "v2-server", "v2-profile", "v2-no-side", "v2-null-side",
        "v2-unknown-target", "v2-minimal"
    })
    void savingAProfileThatHasBeenReadIsStable(String name) throws IOException {
        Path file = files.install(name);

        files.service.save(files.service.getProfile(name).orElseThrow());
        byte[] first = Files.readAllBytes(file);
        Profile loaded = files.service.getProfile(name).orElseThrow();
        files.service.save(loaded);

        // nothing flips between a missing key and null, a profile does not change by being saved
        assertArrayEquals(first, Files.readAllBytes(file));
        assertEquals(loaded, files.service.getProfile(name).orElseThrow());
    }

    @Test
    void typedAndReadTargetDataAreWrittenTheSame() throws IOException {
        Profile typed = new Profile("typed", VersionArg.parse("fabric", "1.21.1"), root.resolve("typed"));

        files.service.save(typed);
        Path file = files.profilesDir.resolve("typed.json");
        byte[] first = Files.readAllBytes(file);
        files.service.save(files.service.getProfile("typed").orElseThrow());

        assertArrayEquals(first, Files.readAllBytes(file));
        assertEquals(handle(typed), handle(files.service.getProfile("typed").orElseThrow()));
    }

}
