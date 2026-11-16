package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.launcher.profile.GameDirLayout;
import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.util.typemap.Key;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The launch target for a version.json file: the file is launched as it is, nothing is resolved or installed.
 */
class VersionFileLaunchTargetTest {
    private static final Account ACCOUNT = new Account("msa", "Steve", "uuid", "token", Account.TYPE_MSA, "xuid");

    @TempDir
    Path root;

    private ClientTargetEnvironment env;
    private VersionFileLaunchTarget target;
    private Path json;

    @BeforeEach
    void setup() {
        env = new ClientTargetEnvironment(root, new FakeProfileService(root.resolve("profiles")));
        target = env.versionFile;
        // a version of another launcher, in the additional directory
        json = env.installCustom(env.secondary, "my-pack");
    }

    private static <V> TypedMap options(Key<V> key, V value) {
        TypedMap options = new TypedMapImpl();
        options.put(key, value);
        return options;
    }

    private Profile profile(Path file) {
        return target.create("p", target.parse(TargetArgs.of(file.toString())), new TypedMapImpl());
    }

    // arguments

    @Test
    void thePathOfAVersionJsonIsParsed() {
        ParsedTarget parsed = target.parse(TargetArgs.of(json.toString()));

        assertEquals(VersionFileLaunchTarget.NAME, parsed.handle().name());
        assertEquals(new VersionFileLaunchTarget.Handle(json), target.handle(parsed.handle()));
        assertEquals(Optional.of(Side.CLIENT), parsed.side());
    }

    @Test
    void thePathIsStoredAbsoluteAndNormalized() {
        Path notNormalized = json.getParent().resolve("..").resolve("my-pack").resolve("my-pack.json");

        assertEquals(json, target.handle(target.parse(TargetArgs.of(notNormalized.toString())).handle()).file());
    }

    @Test
    void theIdOfAnInstalledVersionIsParsedIntoItsFile() {
        ParsedTarget parsed = target.parse(TargetArgs.of("my-pack"));

        // the profile remembers the file, in the directory the version has been found in
        assertEquals(new VersionFileLaunchTarget.Handle(json), target.handle(parsed.handle()));
        assertEquals(Optional.of(Side.CLIENT), parsed.side());
        // an id is only used if nothing else can launch the argument
        assertTrue(parsed.fallback());
        assertFalse(target.parse(TargetArgs.of(json.toString())).fallback());
    }

    @Test
    void thePrimaryDirectoryWinsForAnIdInstalledInSeveralDirectories() {
        Path inPrimary = env.installCustom(env.primary, "my-pack");

        assertEquals(inPrimary, target.handle(target.parse(TargetArgs.of("my-pack")).handle()).file());
        // the other one is launched by its path
        assertEquals(json, target.handle(target.parse(TargetArgs.of(json.toString())).handle()).file());
    }

    @Test
    void aVersionIsFoundByItsIdEvenIfItsFileIsNamedDifferently() throws IOException {
        // copied by hand: the directory is called differently than the version
        Path renamed = env.secondary.getVersionsDir().resolve("copy").resolve("copy.json");
        Files.createDirectories(renamed.getParent());
        Files.writeString(renamed, "Aeromania");

        assertEquals(renamed, target.handle(target.parse(TargetArgs.of("Aeromania")).handle()).file());
        // the name of the file is not an id
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("copy")));
    }

    @Test
    void somethingThatIsNeitherAFileNorAnInstalledIdIsRejected() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of("not-installed"))
        );
        assertTrue(e.getMessage().contains("neither a .json file nor the id of an installed version"), e.getMessage());
        // the name of the file is not the id
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("my-pack.json")));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("")));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("../my-pack")));
    }

    @Test
    void anIdThatIsAlsoAVersionIsLaunchedAsAVersionUnlessTheTargetIsNamed() {
        Path vanilla = env.install(env.primary, "1.21.1");
        Path fabric = env.install(env.primary, "fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD);
        String fabricId = "fabric-loader-" + ClientTargetEnvironment.OLD_BUILD + "-1.21.1";

        // 1.21.1 is a Minecraft version first
        assertSame(env.version, env.launchTargetService.parse(null, TargetArgs.of("1.21.1")).target());
        LaunchTargetService.TargetMatch named = env.launchTargetService.parse(VersionFileLaunchTarget.NAME, TargetArgs.of("1.21.1"));
        assertSame(target, named.target());
        assertEquals(vanilla, target.handle(named.parsed().handle()).file());
        // ids that are not a version by themselves are launched as what they are: that version.json
        LaunchTargetService.TargetMatch byId = env.launchTargetService.parse(null, TargetArgs.of(fabricId));
        assertSame(target, byId.target());
        assertEquals(fabric, target.handle(byId.parsed().handle()).file());
        assertSame(target, env.launchTargetService.parse(null, TargetArgs.of("my-pack")).target());
    }

    @Test
    void filesThatAreNotAVersionJsonAreRejected() throws IOException {
        Path missing = root.resolve("missing.json");
        Path jar = Files.writeString(root.resolve("server.jar"), "jar");
        Path directory = Files.createDirectories(root.resolve("dir.json"));

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of(missing.toString()))
        );
        assertTrue(e.getMessage().contains("does not exist"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(jar.toString())));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(directory.toString())));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of()));
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of(json.toString(), "client")));
    }

    @Test
    void aVersionJsonIsNotAServer() {
        TargetArgs servers = new TargetArgs(List.of(json.toString()), Set.of(Side.SERVER));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> target.parse(servers));

        assertTrue(e.getMessage().contains("launches a client"), e.getMessage());
        assertEquals(List.of(), target.complete(servers));
    }

    @Test
    void theTargetIsDetectedForThePathOfAVersionJson() {
        LaunchTargetService.TargetMatch match = env.launchTargetService.parse(null, TargetArgs.of(json.toString()));

        assertSame(target, match.target());
        // while a version is detected as a version
        assertSame(env.version, env.launchTargetService.parse(null, TargetArgs.of("fabric", "1.21.1")).target());
    }

    @Test
    void installedVersionsOfAllDirectoriesAreCompleted() {
        Path inPrimary = env.install(env.primary, "1.21.1");

        List<ArgCandidate> candidates = target.complete(TargetArgs.of(""));

        // completed with the id, which is enough to launch them
        assertTrue(candidates.contains(new ArgCandidate("my-pack", "installed version")), candidates.toString());
        assertTrue(candidates.contains(new ArgCandidate("1.21.1", "installed version")), candidates.toString());
        assertTrue(Files.exists(inPrimary));
        // an id installed in both directories is listed once
        env.installCustom(env.primary, "my-pack");
        assertEquals(1, target.complete(TargetArgs.of("")).stream().filter(c -> c.value().equals("my-pack")).count());
        assertEquals(List.of(), target.complete(TargetArgs.of(json.toString(), "")));
    }

    // profiles

    @Test
    void aProfileIsAClientInTheDirectoryOfTheFile() {
        Profile profile = profile(json);

        assertEquals("p", profile.name());
        assertEquals(Optional.of(Side.CLIENT), profile.side());
        // the game directory a version called my-pack would get, in the .minecraft directory the file is in
        assertEquals(env.secondary.getMcDir().resolve("my-pack"), profile.path());
        assertEquals("version-file:my-pack.json", target.getLabel(profile));
        // a file does not make a good name for a profile
        assertEquals(Optional.empty(), target.defaultName(profile.target()));
    }

    @Test
    void theGameDirectoryCanBeChosen() {
        ParsedTarget parsed = target.parse(TargetArgs.of(json.toString()));

        Profile custom = target.create("p", parsed, options(LaunchTarget.GAME_DIR, root.resolve("custom")));
        Profile inPrimary = target.create("p", parsed, options(LaunchTarget.MC_FILES, env.primary));

        assertEquals(root.resolve("custom"), custom.path());
        assertEquals(env.primary.getMcDir().resolve("my-pack"), inPrimary.path());
    }

    @Test
    void aFileOutsideOfEveryMinecraftDirectoryCanBeLaunched() throws IOException {
        // laid out like a .minecraft directory, so that is the directory it belongs to
        Path other = root.resolve("other-launcher");
        Path inLayout = other.resolve("versions").resolve("pack").resolve("pack.json");
        Files.createDirectories(inLayout.getParent());
        Files.writeString(inLayout, "pack");
        // just a file: belongs to the primary directory
        Path loose = Files.writeString(root.resolve("loose.json"), "loose");

        assertEquals(other.resolve("pack"), profile(inLayout).path());
        assertEquals(env.primary.getMcDir().resolve("loose"), profile(loose).path());
    }

    @Test
    void aProfileIsAProfileForItsFile() {
        Profile profile = profile(json);
        Path other = env.installCustom(env.secondary, "other-pack");

        assertTrue(target.isSameTarget(profile, target.parse(TargetArgs.of(json.toString()))));
        assertFalse(target.isSameTarget(profile, target.parse(TargetArgs.of(other.toString()))));
    }

    @Test
    void describesTheFileAndTheVersionAPlatformRecognizesItAs() {
        Profile profile = profile(json);

        TargetInfo unknown = target.describe(profile);
        Mockito.when(env.clientLauncher.matchVersionId(Mockito.any()))
            .thenReturn(Optional.of(env.id("fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD)));
        TargetInfo known = target.describe(profile);

        assertEquals(new TargetInfo(Optional.empty(), Optional.empty(), Optional.of(json)), unknown);
        assertEquals(Optional.empty(), known.specified());
        assertEquals(
            Optional.of(VersionArg.parse("fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD)),
            known.current().map(arg -> arg.withSide(null))
        );
    }

    @Test
    void aProfileWhoseFileIsGoneIsStillDescribed() throws IOException {
        Profile profile = profile(json);
        Files.delete(json);

        assertEquals(new TargetInfo(Optional.empty(), Optional.empty(), Optional.of(json)), target.describe(profile));
    }

    // resolving

    @Test
    void resolvingChangesNothingAndInstallsNothing() {
        Profile profile = profile(json);

        assertSame(profile, target.resolve(profile, new TypedMapImpl()));
        assertSame(profile, target.resolve(profile, options(LaunchTarget.UPDATE, true)));
        assertSame(profile, target.resolve(profile, options(LaunchTarget.MC_FILES, env.primary)));
        assertEquals(List.of(), env.installs);
    }

    @Test
    void aNewProfileContainsTheDefaultJvmArguments() {
        Profile resolved = target.resolve(profile(json), options(LaunchTarget.NEW_PROFILE, true));

        assertTrue(resolved.hasDefaultClientJvmArgs());
        assertEquals(profile(json).target(), resolved.target());
    }

    @Test
    void aFileThatHasBeenRemovedIsReported() throws IOException {
        Profile profile = profile(json);
        Files.delete(json);

        LaunchException e = assertThrows(LaunchException.class, () -> target.resolve(profile, new TypedMapImpl()));

        assertTrue(e.getMessage().contains(json.toString()), e.getMessage());
        assertTrue(e.getMessage().contains("of profile p does not exist."), e.getMessage());
    }

    @Test
    void aFileOnADiskThatIsNotMountedIsReported() {
        // the whole directory is gone
        Path gone = root.resolve("unmounted").resolve("versions").resolve("pack").resolve("pack.json");
        Profile profile = profile(json).withTarget(target.handle(new VersionFileLaunchTarget.Handle(gone)));

        LaunchException e = assertThrows(LaunchException.class, () -> target.resolve(profile, new TypedMapImpl()));

        assertTrue(e.getMessage().contains("is the disk of " + root.resolve("unmounted") + " mounted?"), e.getMessage());
    }

    @Test
    void profilesThatAreNotClientsAreRejected() {
        Profile server = profile(json).withSide(Optional.of(Side.SERVER));
        Profile sideless = profile(json).withSide(Optional.empty());

        assertThrows(LaunchException.class, () -> target.resolve(server, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> target.resolve(sideless, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> target.launcher(sideless, options(ClientLauncher.ACCOUNT, ACCOUNT)));
    }

    // launching

    @Test
    void launchesTheFile() {
        Profile profile = profile(json);

        target.launcher(profile, options(ClientLauncher.ACCOUNT, ACCOUNT));

        ArgumentCaptor<Version> version = ArgumentCaptor.forClass(Version.class);
        // no platform knows the version
        Mockito.verify(env.clientLauncher)
            .launcher(Mockito.same(profile), Mockito.eq(ACCOUNT), version.capture(), Mockito.isNull());
        assertEquals("my-pack", version.getValue().getId());
        // the file tells the launcher which .minecraft directory to use
        assertEquals(Optional.of(json), version.getValue().asFile());
        assertEquals(env.secondary, env.versionJsonService.getMcFiles(version.getValue()));
    }

    @Test
    void launchesWithTheVersionAPlatformRecognizesTheFileAs() {
        Path fabric = env.install(env.primary, "fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD);
        Mockito.when(env.clientLauncher.matchVersionId(Mockito.any()))
            .thenReturn(Optional.of(env.id("fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD)));

        target.launcher(profile(fabric), options(ClientLauncher.ACCOUNT, ACCOUNT));

        Mockito.verify(env.clientLauncher).launcher(
            Mockito.any(), Mockito.eq(ACCOUNT), Mockito.any(),
            Mockito.eq(env.id("fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD))
        );
    }

    @Test
    void launchingNeedsAnAccount() {
        LaunchException e = assertThrows(
            LaunchException.class, () -> target.launcher(profile(json), new TypedMapImpl())
        );

        assertTrue(e.getMessage().contains("No account"), e.getMessage());
    }

    @Test
    void theHandleSurvivesBeingStored() {
        Profile profile = profile(json);
        Path file = root.resolve("stored").resolve("profile.json");

        // a profile read from a file has its target data as json, not as the handle of the target
        env.jsonService.write(file, profile, true);
        Profile stored = env.jsonService.parse(file, Profile.class);

        assertEquals(new VersionFileLaunchTarget.Handle(json), target.handle(stored));
        assertTrue(target.isSameTarget(stored, target.parse(TargetArgs.of(json.toString()))));
        assertEquals(Optional.of(json), target.describe(stored).file());
    }

    // the game directory layouts of other launchers, see GameDirLayout

    @Test
    void inTheVersionsLayoutAVersionJsonIsPlayedInItsOwnDirectory() throws IOException {
        // an instance of HMCL or PCL2: the worlds are next to the version.json
        env.gameDirService.withLayout(GameDirLayout.VERSIONS);
        Files.createDirectories(json.getParent().resolve("saves").resolve("world"));

        Profile byPath = profile(json);
        Profile byId = target.create("p", target.parse(TargetArgs.of("my-pack")), new TypedMapImpl());

        assertEquals(json.getParent(), byPath.path());
        assertEquals(json.getParent(), byId.path());
        assertTrue(Files.exists(byId.path().resolve("saves").resolve("world")));
        // resolving does not change it
        assertSame(byPath, target.resolve(byPath, new TypedMapImpl()));
    }

    @Test
    void aLooseVersionJsonHasNoDirectoryToPlayIn() throws IOException {
        env.gameDirService.withLayout(GameDirLayout.VERSIONS);
        Path loose = Files.writeString(root.resolve("loose.json"), "loose");

        // the directory it happens to be in is not a game directory, it gets one like a version of that name
        assertEquals(env.primary.getVersionsDir().resolve("loose"), profile(loose).path());
    }

    @Test
    void inTheVersionsModdedLayoutOnlyVersionsKnownToBeVanillaShareTheMinecraftDirectory() {
        env.gameDirService.withLayout(GameDirLayout.VERSIONS_MODDED);
        Path vanilla = env.install(env.secondary, "1.21.1");
        Path fabric = env.install(env.secondary, "fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD);
        Mockito.when(env.clientLauncher.matchVersionId(Mockito.argThat(v -> v != null && v.getId().equals("1.21.1"))))
            .thenReturn(Optional.of(env.id("1.21.1")));
        Mockito.when(env.clientLauncher.matchVersionId(Mockito.argThat(v -> v != null && v.getId().startsWith("fabric"))))
            .thenReturn(Optional.of(env.id("fabric", "1.21.1", ClientTargetEnvironment.OLD_BUILD)));

        assertEquals(env.secondary.getMcDir(), profile(vanilla).path());
        assertEquals(fabric.getParent(), profile(fabric).path());
        // nothing recognizes my-pack: it keeps its files to itself
        assertEquals(json.getParent(), profile(json).path());
    }

    @Test
    void theGameDirectoryCanStillBeChosenInTheVersionsLayout() {
        env.gameDirService.withLayout(GameDirLayout.VERSIONS);

        Profile custom = target.create(
            "p", target.parse(TargetArgs.of(json.toString())), options(LaunchTarget.GAME_DIR, root.resolve("custom"))
        );

        assertEquals(root.resolve("custom"), custom.path());
    }

}
