package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.LATEST_BUILD;
import static io.github.headlesshq.headlessmc.launcher.client.ClientTargetEnvironment.OLD_BUILD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The launch target for client versions: it finds the version a profile launches, installs it if needed,
 * and pins the profile to the version.json it found.
 */
class VersionLaunchTargetTest {
    private static final Account ACCOUNT = new Account("msa", "Steve", "uuid", "token", Account.TYPE_MSA, "xuid");

    @TempDir
    Path root;

    private FakeProfileService profileService;
    private ClientTargetEnvironment env;
    private VersionLaunchTarget target;

    @BeforeEach
    void setup() {
        profileService = new FakeProfileService(root.resolve("profiles"));
        env = new ClientTargetEnvironment(root, profileService);
        target = env.version;
    }

    private Profile profile(String... version) {
        return new Profile("p", VersionArg.parse(version), root.resolve("game"));
    }

    private Profile pinned(Path json, String[] specified, String[] current) {
        return profile(specified).withTarget(target.handle(
            new VersionHandle(VersionArg.parse(specified), VersionArg.parse(current), json)
        ));
    }

    private Profile resolve(Profile profile) {
        return target.resolve(profile, new TypedMapImpl());
    }

    private static TypedMap options(McFiles mcFiles) {
        TypedMap options = new TypedMapImpl();
        options.put(LaunchTarget.MC_FILES, mcFiles);
        return options;
    }

    private static <V> TypedMap options(io.github.headlesshq.headlessmc.util.typemap.Key<V> key, V value) {
        TypedMap options = new TypedMapImpl();
        options.put(key, value);
        return options;
    }

    private Path fabricJson(McFiles mcFiles, String build) {
        return env.json(mcFiles, "fabric-loader-" + build + "-1.21.1");
    }

    // resolving for the first time

    @Test
    void theFirstResolveInstallsTheVersionAndPinsIt() {
        Profile profile = profile("fabric", "1.21.1");

        Profile resolved = resolve(profile);

        // installed into the primary directory
        assertEquals(List.of(new ClientTargetEnvironment.Install(env.id("fabric", "1.21.1"), env.primary)), env.installs);
        VersionHandle handle = target.handle(resolved);
        // what has been specified is kept
        assertEquals(VersionArg.parse("fabric", "1.21.1"), handle.version());
        // what it resolved to: the build that has been installed
        assertEquals(VersionArg.parse("fabric", "1.21.1", LATEST_BUILD), handle.current());
        assertEquals(fabricJson(env.primary, LATEST_BUILD), handle.currentTarget());
        assertTrue(Files.exists(handle.currentTarget()));
        // nothing else about the profile changed
        assertEquals(profile, resolved.withTarget(profile.target()));
        assertEquals(Optional.of(Side.CLIENT), resolved.side());
    }

    @Test
    void anInstalledVersionIsUsedWithoutInstallingAnything() {
        Path json = env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);

        Profile resolved = resolve(profile("fabric", "1.21.1"));

        assertEquals(List.of(), env.installs);
        assertEquals(json, target.handle(resolved).currentTarget());
        // "fabric 1.21.1" means any build, the installed one is the one it currently is
        assertEquals(VersionArg.parse("fabric", "1.21.1", OLD_BUILD), target.handle(resolved).current());
    }

    @Test
    void aSpecificBuildIsInstalledEvenIfAnotherBuildIsInstalled() {
        env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);

        Profile resolved = resolve(profile("fabric", "1.21.1", LATEST_BUILD));

        assertEquals(1, env.installs.size());
        assertEquals(fabricJson(env.primary, LATEST_BUILD), target.handle(resolved).currentTarget());
    }

    @Test
    void latestIsResolvedToTheBuildThatIsTheLatestNow() {
        Profile resolved = resolve(profile("fabric", "1.21.1", "latest"));

        VersionHandle handle = target.handle(resolved);
        assertEquals(Optional.of("latest"), handle.version().build());
        assertEquals(VersionArg.parse("fabric", "1.21.1", LATEST_BUILD), handle.current());
    }

    @Test
    void aVanillaVersionIsResolved() {
        Profile resolved = resolve(profile("1.21.1"));

        assertEquals(env.json(env.primary, "1.21.1"), target.handle(resolved).currentTarget());
        assertEquals(VersionArg.parse("1.21.1"), target.handle(resolved).current());
    }

    // the pin

    @Test
    void aSecondResolveChangesNothingAndInstallsNothing() {
        Profile resolved = resolve(profile("fabric", "1.21.1"));

        Profile again = resolve(resolved);

        // the very same profile, so that nothing is saved
        assertSame(resolved, again);
        assertEquals(1, env.installs.size());
    }

    @Test
    void aPinnedProfileIsNotMovedToANewerBuild() {
        // pinned when 0.16.9 was the latest build, in the meantime 0.16.10 has been released
        Path json = env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        Profile profile = pinned(
            json, new String[]{"fabric", "1.21.1", "latest"}, new String[]{"fabric", "1.21.1", OLD_BUILD}
        );

        Profile resolved = resolve(profile);

        assertSame(profile, resolved);
        assertEquals(List.of(), env.installs);
    }

    @Test
    void aPinnedProfileIsNotMovedToANewerBuildThatIsInstalled() {
        Path json = env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        env.install(env.primary, "fabric", "1.21.1", LATEST_BUILD);
        Profile profile = pinned(json, new String[]{"fabric", "1.21.1"}, new String[]{"fabric", "1.21.1", OLD_BUILD});

        assertSame(profile, resolve(profile));
    }

    @Test
    void updateResolvesWhatHasBeenSpecifiedAgain() {
        Path json = env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        Profile profile = pinned(
            json, new String[]{"fabric", "1.21.1", "latest"}, new String[]{"fabric", "1.21.1", OLD_BUILD}
        );

        Profile updated = target.resolve(profile, options(LaunchTarget.UPDATE, true));

        VersionHandle handle = target.handle(updated);
        assertEquals(Optional.of("latest"), handle.version().build());
        assertEquals(VersionArg.parse("fabric", "1.21.1", LATEST_BUILD), handle.current());
        assertEquals(fabricJson(env.primary, LATEST_BUILD), handle.currentTarget());
        assertEquals(1, env.installs.size());
        // the old build is still installed
        assertTrue(Files.exists(json));
    }

    @Test
    void updateOfAProfileThatIsUpToDateChangesNothing() {
        Profile resolved = resolve(profile("fabric", "1.21.1", "latest"));

        Profile updated = target.resolve(resolved, options(LaunchTarget.UPDATE, true));

        assertEquals(target.handle(resolved), target.handle(updated));
        assertEquals(1, env.installs.size());
    }

    @Test
    void aPinThatHasBeenRemovedIsResolvedAgain() throws IOException {
        Profile resolved = resolve(profile("fabric", "1.21.1"));
        Path pin = target.handle(resolved).currentTarget();
        // the version has been removed, the .minecraft directory is still there
        Files.delete(pin);

        Profile again = resolve(resolved);

        assertEquals(2, env.installs.size());
        assertTrue(Files.exists(target.handle(again).currentTarget()));
        // the build it had is installed again, not whatever is the latest
        assertEquals(target.handle(resolved).current(), target.handle(again).current());
    }

    @Test
    void aBrokenPinKeepsItsBuild() throws IOException {
        Path json = env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        Profile profile = pinned(json, new String[]{"fabric", "1.21.1"}, new String[]{"fabric", "1.21.1", OLD_BUILD});
        Files.delete(json);

        Profile resolved = resolve(profile);

        assertEquals(VersionArg.parse("fabric", "1.21.1", OLD_BUILD), target.handle(resolved).current());
        assertEquals(fabricJson(env.primary, OLD_BUILD), target.handle(resolved).currentTarget());
    }

    @Test
    void aPinIntoADirectoryThatIsGoneIsNotSilentlyInstalledElsewhere() {
        // e.g. a disk that is not mounted
        Path gone = root.resolve("unmounted").resolve("versions").resolve("fabric").resolve("fabric.json");
        Profile profile = pinned(gone, new String[]{"fabric", "1.21.1"}, new String[]{"fabric", "1.21.1", OLD_BUILD});

        LaunchException e = assertThrows(LaunchException.class, () -> resolve(profile));

        assertTrue(e.getMessage().contains("is its disk mounted"), e.getMessage());
        assertTrue(e.getMessage().contains(root.resolve("unmounted").toString()), e.getMessage());
        assertTrue(e.getMessage().contains("profile update p --mc"), e.getMessage());
        assertEquals(List.of(), env.installs);
    }

    // multiple .minecraft directories

    @Test
    void thePrimaryDirectoryWinsForAVersionInstalledInBoth() {
        Path secondary = env.install(env.secondary, "fabric", "1.21.1", OLD_BUILD);
        Path primary = env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);

        Profile resolved = resolve(profile("fabric", "1.21.1"));

        assertEquals(primary, target.handle(resolved).currentTarget());
        assertNotEquals(secondary, primary);
    }

    @Test
    void aPinIsStableWhenTheOrderOfTheDirectoriesChanges() {
        env.install(env.secondary, "fabric", "1.21.1", OLD_BUILD);
        env.install(env.primary, "fabric", "1.21.1", OLD_BUILD);
        Profile resolved = resolve(profile("fabric", "1.21.1"));

        // the other directory is the primary one now
        ClientTargetEnvironment swapped = new ClientTargetEnvironment(root, profileService, null, env.secondary, env.primary);
        swapped.install(swapped.primary, "fabric", "1.21.1", OLD_BUILD);
        swapped.install(swapped.secondary, "fabric", "1.21.1", OLD_BUILD);

        assertSame(resolved, swapped.version.resolve(resolved, new TypedMapImpl()));
        // an unpinned profile would get the other one
        assertEquals(
            fabricJson(env.secondary, OLD_BUILD),
            swapped.version.handle(swapped.version.resolve(profile("fabric", "1.21.1"), new TypedMapImpl())).currentTarget()
        );
    }

    @Test
    void aVersionOnlyInstalledInAnAdditionalDirectoryIsFound() {
        Path json = env.install(env.secondary, "fabric", "1.21.1", OLD_BUILD);

        Profile resolved = resolve(profile("fabric", "1.21.1"));

        assertEquals(json, target.handle(resolved).currentTarget());
        assertEquals(List.of(), env.installs);
    }

    @Test
    void aRequestedDirectoryIsInstalledInto() {
        Profile resolved = target.resolve(profile("fabric", "1.21.1"), options(env.secondary));

        assertEquals(env.secondary, env.installs.getFirst().mcFiles());
        assertEquals(fabricJson(env.secondary, LATEST_BUILD), target.handle(resolved).currentTarget());
        assertFalse(Files.exists(env.primary.getMcDir()));
    }

    @Test
    void aRequestedDirectoryIsUsedEvenIfTheVersionIsInstalledInAnother() {
        env.install(env.primary, "fabric", "1.21.1", LATEST_BUILD);

        Profile resolved = target.resolve(profile("fabric", "1.21.1"), options(env.secondary));

        assertEquals(fabricJson(env.secondary, LATEST_BUILD), target.handle(resolved).currentTarget());
        assertEquals(1, env.installs.size());
    }

    @Test
    void aPinnedProfileCanBeMovedToAnotherDirectory() {
        Profile resolved = resolve(profile("fabric", "1.21.1"));
        assertEquals(fabricJson(env.primary, LATEST_BUILD), target.handle(resolved).currentTarget());

        Profile moved = target.resolve(resolved, options(env.secondary));

        // the same build, in the other directory
        assertEquals(fabricJson(env.secondary, LATEST_BUILD), target.handle(moved).currentTarget());
        assertEquals(target.handle(resolved).current(), target.handle(moved).current());
        // and requesting the directory it already is in changes nothing
        assertSame(moved, target.resolve(moved, options(env.secondary)));
    }

    @Test
    void aBrokenPinIsReinstalledIntoTheDirectoryItWasIn() throws IOException {
        Profile resolved = target.resolve(profile("fabric", "1.21.1"), options(env.secondary));
        Files.delete(target.handle(resolved).currentTarget());

        Profile again = resolve(resolved);

        assertEquals(env.secondary, env.installs.getLast().mcFiles());
        assertEquals(fabricJson(env.secondary, LATEST_BUILD), target.handle(again).currentTarget());
    }

    // a platform that is not available anymore

    @Test
    void aPinnedProfileCanBeLaunchedWithoutItsPlatform() {
        Profile resolved = resolve(profile("fabric", "1.21.1"));
        // e.g. the plugin that provided the platform has been removed
        env.removeFabric();
        TypedMap options = options(ClientLauncher.ACCOUNT, ACCOUNT);
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        Mockito.when(env.clientLauncher.launcher(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(processLauncher);

        // the profile stays as it is, with its version, for when the platform is back
        Profile again = target.resolve(resolved, options);
        assertSame(resolved, again);

        assertSame(processLauncher, target.launcher(again, options));
        ArgumentCaptor<Version> version = ArgumentCaptor.forClass(Version.class);
        // launched from the pinned version.json, without the id the platform would give it
        Mockito.verify(env.clientLauncher).launcher(Mockito.same(again), Mockito.eq(ACCOUNT), version.capture(), Mockito.isNull());
        assertEquals("fabric-loader-" + LATEST_BUILD + "-1.21.1", version.getValue().getId());
        assertEquals(Optional.of(target.handle(resolved).currentTarget()), version.getValue().asFile());
        assertEquals(1, env.installs.size());
    }

    @Test
    void anUnpinnedProfileCannotBeLaunchedWithoutItsPlatform() {
        Profile profile = profile("fabric", "1.21.1");
        env.removeFabric();

        LaunchException e = assertThrows(LaunchException.class, () -> resolve(profile));

        assertTrue(e.getMessage().contains("Failed to find platform fabric"), e.getMessage());
        assertTrue(e.getMessage().contains("plugin"), e.getMessage());
        assertTrue(e.getMessage().contains("not pinned"), e.getMessage());
    }

    @Test
    void aBrokenPinCannotBeRepairedWithoutItsPlatform() throws IOException {
        Profile resolved = resolve(profile("fabric", "1.21.1"));
        Files.delete(target.handle(resolved).currentTarget());
        env.removeFabric();

        assertThrows(LaunchException.class, () -> resolve(resolved));
    }

    @Test
    void aProfileOfAnUnavailablePlatformIsStillDescribed() {
        Profile resolved = resolve(profile("fabric", "1.21.1"));
        env.removeFabric();

        TargetInfo info = target.describe(resolved);

        assertEquals(Optional.of(VersionArg.parse("fabric", "1.21.1")), info.specified());
        assertEquals(Optional.of(target.handle(resolved).currentTarget()), info.file());
    }

    // installing

    @Test
    void missingVersionsAreNotInstalledIfAutoInstallIsDisabled() {
        Mockito.when(env.config.autoInstall()).thenReturn(false);

        LaunchException e = assertThrows(LaunchException.class, () -> resolve(profile("fabric", "1.21.1")));

        assertTrue(e.getMessage().contains("Failed to find installed version fabric/1.21.1"), e.getMessage());
        assertTrue(e.getMessage().contains("headlessmc install fabric 1.21.1"), e.getMessage());
        assertEquals(List.of(), env.installs);
    }

    @Test
    void aNewProfileIsInstalledEvenIfAutoInstallIsDisabled() {
        // adding a profile is asking for its version to be installed
        Mockito.when(env.config.autoInstall()).thenReturn(false);

        Profile resolved = target.resolve(profile("fabric", "1.21.1"), options(LaunchTarget.NEW_PROFILE, true));

        assertEquals(1, env.installs.size());
        assertNotNull(target.handle(resolved).currentTarget());
    }

    @Test
    void aNewProfileContainsTheDefaultJvmArgumentsOfItsVersion() {
        Profile profile = profile("fabric", "1.21.1").withVmArgs(List.of("-Xmx2G"));

        Profile resolved = target.resolve(profile, options(LaunchTarget.NEW_PROFILE, true));

        // they are in the profile now, to be seen and edited, and are not added again on launch
        assertTrue(resolved.hasDefaultClientJvmArgs());
        assertEquals(List.of("-Xmx2G"), resolved.vmArgs());
        // an existing profile is not touched
        assertFalse(resolve(profile).hasDefaultClientJvmArgs());
    }

    @Test
    void aForcedInstallReinstallsAnInstalledVersion() {
        Profile resolved = resolve(profile("fabric", "1.21.1"));

        Profile forced = target.resolve(resolved, options(ClientInstaller.FORCE_INSTALL, true));

        assertEquals(2, env.installs.size());
        assertEquals(target.handle(resolved).currentTarget(), target.handle(forced).currentTarget());
    }

    @Test
    void theOptionsOfTheResolveAreGivenToTheInstaller() {
        target.resolve(profile("fabric", "1.21.1"), options(env.secondary));

        assertEquals(env.secondary, env.installs.getFirst().mcFiles());
        assertEquals(env.id("fabric", "1.21.1"), env.installs.getFirst().id());
    }

    // launching

    @Test
    void launchesThePinnedVersionWithTheAccount() {
        TypedMap options = options(ClientLauncher.ACCOUNT, ACCOUNT);
        Profile resolved = target.resolve(profile("fabric", "1.21.1"), options);
        Profile prepared = resolved.withVmArgs(List.of("-Xmx8G"));

        target.launcher(prepared, options);

        ArgumentCaptor<Version> version = ArgumentCaptor.forClass(Version.class);
        // the profile with the options of this launch, the version that has been resolved, and its id
        Mockito.verify(env.clientLauncher).launcher(
            Mockito.same(prepared), Mockito.eq(ACCOUNT), version.capture(),
            Mockito.eq(env.id("fabric", "1.21.1", LATEST_BUILD))
        );
        assertEquals(Optional.of(target.handle(resolved).currentTarget()), version.getValue().asFile());
        assertEquals(1, env.installs.size());
    }

    @Test
    void launchingAResolvedProfileWorksWithoutResolvingItInTheSameRun() {
        Profile resolved = resolve(profile("fabric", "1.21.1"));
        // another instance of the target, as after a restart
        ClientTargetEnvironment restarted = new ClientTargetEnvironment(root, profileService);
        restarted.install(restarted.primary, "fabric", "1.21.1", LATEST_BUILD);

        restarted.version.launcher(resolved, options(ClientLauncher.ACCOUNT, ACCOUNT));

        Mockito.verify(restarted.clientLauncher).launcher(
            Mockito.any(), Mockito.eq(ACCOUNT),
            Mockito.argThat(version -> version.asFile().equals(Optional.of(fabricJson(env.primary, LATEST_BUILD)))),
            Mockito.any()
        );
        assertEquals(List.of(), restarted.installs);
    }

    @Test
    void launchingNeedsAnAccount() {
        Profile resolved = resolve(profile("fabric", "1.21.1"));

        LaunchException e = assertThrows(LaunchException.class, () -> target.launcher(resolved, new TypedMapImpl()));

        assertTrue(e.getMessage().contains("No account"), e.getMessage());
    }

    @Test
    void launchingDoesNotInstall() {
        // not resolved, so nothing is installed
        LaunchException e = assertThrows(
            LaunchException.class,
            () -> target.launcher(profile("fabric", "1.21.1"), options(ClientLauncher.ACCOUNT, ACCOUNT))
        );

        assertTrue(e.getMessage().contains("is not installed"), e.getMessage());
        assertEquals(List.of(), env.installs);
    }

    // invalid profiles

    @Test
    void profilesThatAreNotClientsAreRejected() {
        Profile server = new Profile("srv", VersionArg.parse("server", "1.21.1"), root)
            .withTarget(target.handle(VersionHandle.of(VersionArg.parse("1.21.1"))));
        Profile sideless = profile("1.21.1").withSide(Optional.empty());

        assertThrows(LaunchException.class, () -> resolve(server));
        assertThrows(LaunchException.class, () -> resolve(sideless));
        assertThrows(LaunchException.class, () -> target.launcher(sideless, options(ClientLauncher.ACCOUNT, ACCOUNT)));
        assertEquals(List.of(), env.installs);
    }

    @Test
    void aVersionThatDoesNotExistAnymoreIsReported() {
        // e.g. a profile that has been edited by hand
        Profile profile = profile("fabric", "1.7.10");

        LaunchException e = assertThrows(LaunchException.class, () -> resolve(profile));

        assertTrue(e.getMessage().contains("fabric/1.7.10"), e.getMessage());
        assertTrue(e.getMessage().contains("profile p"), e.getMessage());
    }

    // arguments

    @Test
    void onlyVersionsOfPlatformsWithClientsAreParsed() {
        assertDoesNotThrow(() -> target.parse(TargetArgs.of("fabric", "1.21.1")));
        assertDoesNotThrow(() -> target.parse(TargetArgs.of("1.21.1")));
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of("paper", "1.21.1"))
        );
        assertTrue(e.getMessage().contains("platform paper does not support clients"), e.getMessage());
        // neither a path nor the name of an installed version.json
        Path json = env.installCustom(env.primary, "my-pack");
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("my-pack")));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(json.toString())));
    }

    @Test
    void theGameDirectoryOfANewProfileIsInTheDirectoryItIsInstalledInto() {
        Profile inPrimary = target.create("a", target.parse(TargetArgs.of("fabric", "1.21.1")), new TypedMapImpl());
        Profile inSecondary = target.create("b", target.parse(TargetArgs.of("fabric", "1.21.1")), options(env.secondary));
        Profile custom = target.create(
            "c", target.parse(TargetArgs.of("fabric", "1.21.1")), options(LaunchTarget.GAME_DIR, root.resolve("custom"))
        );

        assertEquals(env.primary.getMcDir().resolve("fabric-1.21.1"), inPrimary.path());
        assertEquals(env.secondary.getMcDir().resolve("fabric-1.21.1"), inSecondary.path());
        assertEquals(root.resolve("custom"), custom.path());
        // it is decided when the profile is created, resolving does not change it
        assertEquals(inPrimary.path(), target.resolve(inPrimary, options(env.secondary)).path());
    }

}
