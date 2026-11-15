package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstallation;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.util.typemap.Key;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The launch target for servers of a version: a server is installed into the game directory of its profile
 * when the profile is added, and the profile is pinned to the executable that has been installed.
 */
class ServerLaunchTargetTest {
    private static final String LATEST_BUILD = "130";
    private static final String OLD_BUILD = "120";
    private static final int INSTALLED_JAVA = 21;
    private static final int FOUND_JAVA = 17;

    @TempDir
    Path root;

    /** A server that has been installed by the fake platform. */
    private record Install(VersionArg version, Path dir) {
    }

    /** The platforms can be taken away, like the platform of a plugin that has been removed. */
    private static final class Platforms implements PlatformService {
        private FakePlatformService delegate;

        @Override
        public List<Platform> getPlatforms() {
            return delegate.getPlatforms();
        }

        @Override
        public List<Platform> getPlatformsWithoutVanilla() {
            return delegate.getPlatformsWithoutVanilla();
        }

        @Override
        public Optional<Platform> getPlatform(String name) {
            return delegate.getPlatform(name);
        }

        @Override
        public VanillaPlatform getVanillaPlatform() {
            return delegate.getVanillaPlatform();
        }
    }

    private final List<Install> installs = new ArrayList<>();
    private boolean reportBuild = true;
    private final Platforms platforms = new Platforms();
    private final ServerLauncher serverLauncher = Mockito.mock(ServerLauncher.class);
    private final JsonService jsonService = new DefaultJacksonJsonService();
    private FakeVanillaPlatform vanilla;
    private AppFiles appFiles;
    private ServerLaunchTarget target;

    @BeforeEach
    void setup() {
        ServerFinder finder = dir -> {
            Path jar = dir.resolve(ServerFinder.DEFAULT_JAR);
            if (!Files.isRegularFile(jar)) {
                throw new HeadlessMcException("no server.jar in " + dir);
            }

            return jar;
        };
        ServerSupport support = new ServerSupport(finder, this::install);
        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4").withServerSupport(support);
        FakePlatform paper = FakePlatform.create("paper", new String[]{"1.21.1", LATEST_BUILD, OLD_BUILD})
            .withServerSupport(support);
        // a platform that only has clients
        FakePlatform liteloader = FakePlatform.create("liteloader", new String[]{"1.21.1", "1"});
        platforms.delegate = new FakePlatformService(vanilla, paper, liteloader);
        appFiles = TestFiles.appFiles(root);
        Mockito.when(serverLauncher.findJavaVersion(Mockito.any())).thenReturn(FOUND_JAVA);
        target = new ServerLaunchTarget(platforms, serverLauncher, jsonService, appFiles);
    }

    private ServerInstallation install(VersionID id, Path dir, TypedMap args) {
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(ServerFinder.DEFAULT_JAR), id.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        installs.add(new Install(id.asArg().withSide(null), dir));
        // like the installer of paper: without a build the latest one is installed, and reported
        String build = reportBuild ? id.getBuild().map(PlatformVersion::getName).orElse(LATEST_BUILD) : null;
        return new ServerInstallation(INSTALLED_JAVA, List.of("-Dinstalled=true"), build);
    }

    private static <V> TypedMap options(Key<V> key, V value) {
        TypedMap options = new TypedMapImpl();
        options.put(key, value);
        return options;
    }

    private static TargetArgs servers(String... args) {
        return new TargetArgs(List.of(args), Set.of(Side.SERVER));
    }

    private Profile profile(String... version) {
        return target.create("srv", target.parse(servers(version)), new TypedMapImpl());
    }

    private Profile added(String... version) {
        return target.resolve(profile(version), options(LaunchTarget.NEW_PROFILE, true));
    }

    private Path jar(Profile profile) {
        return profile.path().resolve(ServerFinder.DEFAULT_JAR).toAbsolutePath().normalize();
    }

    // arguments

    @Test
    void aServerVersionIsParsed() {
        ParsedTarget parsed = target.parse(TargetArgs.of("server", "paper", "1.21.1"));

        assertEquals(VersionHandle.SERVER, parsed.handle().name());
        assertEquals(Optional.of(Side.SERVER), parsed.side());
        assertEquals(VersionHandle.of(VersionArg.parse("server", "paper", "1.21.1")), target.handle(parsed.handle()));
    }

    @Test
    void aVersionWithoutServerIsAClientUnlessTheCommandOnlyLaunchesServers() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(TargetArgs.of("paper", "1.21.1"))
        );
        assertTrue(e.getMessage().contains("is a client version, use 'server paper 1.21.1'"), e.getMessage());

        // e.g. "server add paper 1.21.1"
        assertEquals(target.parse(TargetArgs.of("server", "paper", "1.21.1")), target.parse(servers("paper", "1.21.1")));
        assertEquals(target.parse(TargetArgs.of("server", "1.21.1")), target.parse(servers("1.21.1")));
    }

    @Test
    void versionsThatAreNotServersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> target.parse(TargetArgs.of("client", "paper", "1.21.1")));
        assertThrows(IllegalArgumentException.class, () -> target.parse(servers("client", "paper", "1.21.1")));
        // a command that only launches clients
        assertThrows(
            IllegalArgumentException.class,
            () -> target.parse(new TargetArgs(List.of("server", "paper", "1.21.1"), Set.of(Side.CLIENT)))
        );
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> target.parse(servers("liteloader", "1.21.1"))
        );
        assertTrue(e.getMessage().contains("platform liteloader does not support servers"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> target.parse(servers("paper", "1.7.10")));
        assertThrows(IllegalArgumentException.class, () -> target.parse(servers("quilt", "1.21.1")));
        assertThrows(IllegalArgumentException.class, () -> target.parse(servers()));
    }

    // profiles

    @Test
    void aServerProfileLivesInTheServerDirectory() {
        Profile profile = profile("paper", "1.21.1");

        assertEquals("srv", profile.name());
        assertEquals(Optional.of(Side.SERVER), profile.side());
        assertEquals(appFiles.getServerDir().resolve("srv"), profile.path());
        assertEquals(List.of("nogui"), profile.gameArgs());
        assertEquals(Optional.of("server-paper-1.21.1"), target.defaultName(profile.target()));
        // creating a profile installs nothing
        assertEquals(List.of(), installs);
        assertFalse(Files.exists(profile.path()));
    }

    @Test
    void theGameDirectoryCanBeChosen() {
        Profile profile = target.create(
            "srv", target.parse(servers("paper", "1.21.1")), options(LaunchTarget.GAME_DIR, root.resolve("custom"))
        );

        assertEquals(root.resolve("custom"), profile.path());
    }

    @Test
    void aProfileIsAProfileForItsVersion() {
        Profile added = added("paper", "1.21.1");

        // what has been specified counts, not the build it resolved to
        assertTrue(target.isSameTarget(added, target.parse(servers("paper", "1.21.1"))));
        assertFalse(target.isSameTarget(added, target.parse(servers("paper", "1.21.1", LATEST_BUILD))));
        assertFalse(target.isSameTarget(added, target.parse(servers("1.21.1"))));
    }

    // adding a server

    @Test
    void addingAServerInstallsItIntoItsGameDirectory() {
        Profile profile = profile("paper", "1.21.1");

        Profile added = target.resolve(profile, options(LaunchTarget.NEW_PROFILE, true));

        assertEquals(List.of(new Install(VersionArg.parse("paper", "1.21.1"), profile.path())), installs);
        VersionHandle handle = target.handle(added);
        // what has been specified is kept, the build the installer reports is what it currently is
        assertEquals(VersionArg.parse("server", "paper", "1.21.1"), handle.version());
        assertEquals(VersionArg.parse("server", "paper", "1.21.1", LATEST_BUILD), handle.current());
        // pinned to the executable that has been installed
        assertEquals(jar(profile), handle.currentTarget());
        // java and jvm arguments are those the installer reports
        assertEquals(INSTALLED_JAVA, added.javaVersion());
        assertEquals(List.of("-Dinstalled=true"), added.vmArgs());
        assertEquals(profile.path(), added.path());
        assertEquals(Optional.of(Side.SERVER), added.side());
    }

    @Test
    void anInstallerThatCannotTellItsBuildLeavesTheBuildOpen() {
        // e.g. the installer of fabric picks the loader itself
        reportBuild = false;

        Profile added = added("paper", "1.21.1");

        assertEquals(VersionArg.parse("server", "paper", "1.21.1"), target.handle(added).current());
        assertEquals(jar(added), target.handle(added).currentTarget());
    }

    @Test
    void aSpecificBuildIsInstalled() {
        Profile added = added("paper", "1.21.1", OLD_BUILD);

        assertEquals(VersionArg.parse("paper", "1.21.1", OLD_BUILD), installs.getFirst().version());
        assertEquals(VersionArg.parse("server", "paper", "1.21.1", OLD_BUILD), target.handle(added).current());
    }

    // launching an existing server

    @Test
    void anAddedServerIsNotInstalledAgain() {
        Profile added = added("paper", "1.21.1");

        Profile resolved = target.resolve(added, new TypedMapImpl());

        // the very same profile, so that nothing is saved
        assertSame(added, resolved);
        assertEquals(1, installs.size());
    }

    @Test
    void aServerThatIsNotInstalledIsNotInstalledByLaunchingIt() {
        Profile profile = profile("paper", "1.21.1");

        LaunchException e = assertThrows(LaunchException.class, () -> target.resolve(profile, new TypedMapImpl()));

        assertTrue(e.getMessage().contains("Failed to find the server of profile srv"), e.getMessage());
        assertTrue(e.getMessage().contains("server add paper 1.21.1"), e.getMessage());
        assertEquals(List.of(), installs);
    }

    @Test
    void aProfileFromAnOlderFormatIsPinnedToTheServerThatIsInstalled() throws IOException {
        // an old profile only knows its version, its server has been installed long ago
        Profile old = profile("paper", "1.21.1");
        Files.createDirectories(old.path());
        Files.writeString(old.path().resolve(ServerFinder.DEFAULT_JAR), "installed");

        Profile resolved = target.resolve(old, new TypedMapImpl());

        VersionHandle handle = target.handle(resolved);
        assertEquals(jar(old), handle.currentTarget());
        // which build is installed is not known
        assertEquals(VersionArg.parse("server", "paper", "1.21.1"), handle.current());
        // without a java version in the profile, one is found for the version
        assertEquals(FOUND_JAVA, resolved.javaVersion());
        assertEquals(List.of(), installs);
        // and then nothing changes anymore
        assertSame(resolved, target.resolve(resolved, new TypedMapImpl()));
    }

    @Test
    void theJavaVersionOfAProfileIsKept() throws IOException {
        Profile old = profile("paper", "1.21.1").withJavaVersion(8);
        Files.createDirectories(old.path());
        Files.writeString(old.path().resolve(ServerFinder.DEFAULT_JAR), "installed");

        assertEquals(8, target.resolve(old, new TypedMapImpl()).javaVersion());
        Mockito.verify(serverLauncher, Mockito.never()).findJavaVersion(Mockito.any());
    }

    @Test
    void aPinThatIsGoneIsFoundAgain() throws IOException {
        Profile added = added("paper", "1.21.1");
        Path script = Files.writeString(added.path().resolve("run.sh"), "java -jar server.jar");
        Profile pinnedToScript = added.withTarget(
            target.handle(target.handle(added).withCurrentTarget(script.toAbsolutePath().normalize()))
        );

        // the pin exists: it is launched, whatever the platform would find
        assertSame(pinnedToScript, target.resolve(pinnedToScript, new TypedMapImpl()));
        Files.delete(script);
        Profile resolved = target.resolve(pinnedToScript, new TypedMapImpl());

        assertEquals(jar(added), target.handle(resolved).currentTarget());
        assertEquals(1, installs.size());
    }

    @Test
    void updateInstallsWhatHasBeenSpecifiedAgain() {
        Profile added = added("paper", "1.21.1", OLD_BUILD);
        // the user edits the version of the profile
        Profile edited = added
            .withTarget(target.handle(new VersionHandle(
                VersionArg.parse("server", "paper", "1.21.1", "latest"),
                target.handle(added).current(),
                target.handle(added).currentTarget()
            )))
            .withVmArgs(List.of("-Xmx8G"))
            .withJavaVersion(8);

        Profile updated = target.resolve(edited, options(LaunchTarget.UPDATE, true));

        // latest is resolved to the build that is the latest now
        assertEquals(VersionArg.parse("paper", "1.21.1", LATEST_BUILD), installs.getLast().version());
        assertEquals(2, installs.size());
        VersionHandle handle = target.handle(updated);
        assertEquals(Optional.of("latest"), handle.version().build());
        assertEquals(VersionArg.parse("server", "paper", "1.21.1", LATEST_BUILD), handle.current());
        assertEquals(jar(added), handle.currentTarget());
        // the new server may need another java, the jvm arguments of the user are kept
        assertEquals(INSTALLED_JAVA, updated.javaVersion());
        assertEquals(List.of("-Xmx8G"), updated.vmArgs());
    }

    // a platform that is not available anymore

    @Test
    void aPinnedServerCanBeLaunchedWithoutItsPlatform() {
        Profile added = added("paper", "1.21.1");
        // e.g. the plugin that provided the platform has been removed
        platforms.delegate = new FakePlatformService(vanilla);
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        TypedMap options = new TypedMapImpl();
        Mockito.when(serverLauncher.launcher(added, jar(added), options)).thenReturn(processLauncher);

        // the profile stays as it is, with its version, for when the platform is back
        assertSame(added, target.resolve(added, options));
        assertSame(processLauncher, target.launcher(added, options));
        assertEquals(1, installs.size());
        // and it can still be listed
        TargetInfo info = target.describe(added);
        assertEquals(Optional.of(VersionArg.parse("server", "paper", "1.21.1")), info.specified());
        assertEquals(Optional.of(jar(added)), info.file());
    }

    @Test
    void anUnpinnedServerCannotBeLaunchedWithoutItsPlatform() throws IOException {
        Profile old = profile("paper", "1.21.1");
        Files.createDirectories(old.path());
        Files.writeString(old.path().resolve(ServerFinder.DEFAULT_JAR), "installed");
        platforms.delegate = new FakePlatformService(vanilla);

        LaunchException e = assertThrows(LaunchException.class, () -> target.resolve(old, new TypedMapImpl()));

        assertTrue(e.getMessage().contains("Failed to resolve version"), e.getMessage());
        assertTrue(e.getMessage().contains("is the plugin still installed?"), e.getMessage());
        assertTrue(e.getMessage().contains("not pinned"), e.getMessage());
    }

    @Test
    void aServerCannotBeAddedOrUpdatedWithoutItsPlatform() {
        Profile profile = profile("paper", "1.21.1");
        Profile added = added("paper", "1.21.1");
        platforms.delegate = new FakePlatformService(vanilla);

        assertThrows(LaunchException.class, () -> target.resolve(profile, options(LaunchTarget.NEW_PROFILE, true)));
        assertThrows(LaunchException.class, () -> target.resolve(added, options(LaunchTarget.UPDATE, true)));
        assertEquals(1, installs.size());
    }

    // launching

    @Test
    void launchesThePinnedExecutable() {
        Profile added = added("paper", "1.21.1");
        Profile prepared = added.withVmArgs(List.of("-Xmx8G"));
        TypedMap options = options(ServerLauncher.PROCESS_NAME, "srv");
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        Mockito.when(serverLauncher.launcher(prepared, jar(added), options)).thenReturn(processLauncher);

        assertSame(processLauncher, target.launcher(prepared, options));
    }

    @Test
    void launchingAServerThatIsNotInstalledFails() {
        assertThrows(LaunchException.class, () -> target.launcher(profile("paper", "1.21.1"), new TypedMapImpl()));
        assertEquals(List.of(), installs);
    }

    @Test
    void profilesThatAreNotServersAreRejected() {
        Profile client = added("paper", "1.21.1").withSide(Optional.of(Side.CLIENT));
        Profile sideless = added("paper", "1.21.1").withSide(Optional.empty());

        assertThrows(LaunchException.class, () -> target.resolve(client, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> target.resolve(sideless, new TypedMapImpl()));
        assertThrows(LaunchException.class, () -> target.launcher(sideless, new TypedMapImpl()));
    }

    // moving

    @Test
    void thePinMovesWithTheGameDirectory() {
        Profile added = added("paper", "1.21.1");
        Path moved = root.resolve("moved");

        VersionHandle handle = target.handle(target.relocate(added, moved));

        assertEquals(moved.resolve(ServerFinder.DEFAULT_JAR), handle.currentTarget());
        assertEquals(target.handle(added).version(), handle.version());
        assertEquals(target.handle(added).current(), handle.current());
        // a profile that is not pinned has nothing to move
        Profile unpinned = profile("paper", "1.21.1");
        assertEquals(target.handle(unpinned), target.handle(target.relocate(unpinned, moved)));
    }

    @Test
    void theHandleSurvivesBeingStored() {
        Profile added = added("paper", "1.21.1");
        Path file = root.resolve("stored").resolve("profile.json");

        jsonService.write(file, added, true);
        Profile stored = jsonService.parse(file, Profile.class);

        assertEquals(target.handle(added), target.handle(stored));
        assertSame(stored, target.resolve(stored, new TypedMapImpl()));
    }

}
