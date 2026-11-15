package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessException;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.ProcessLifecycle;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.FakeLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.jspecify.annotations.Nullable;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.Eula;
import io.github.headlesshq.headlessmc.launcher.server.EulaLauncher;
import io.github.headlesshq.headlessmc.launcher.server.FakeServerService;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
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

import static org.junit.jupiter.api.Assertions.*;

class ServerLaunchCommandsTest {
    /** Thrown instead of actually starting a server process. */
    private static final class Launched extends RuntimeException {
        private final transient Profile profile;

        Launched(Profile profile) {
            this.profile = profile;
        }
    }

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final List<Profile> eulaLaunches = new ArrayList<>();

    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private FakeServerService serverService;
    private ProfileResolver profileResolver;
    private ServerLauncher serverLauncher;
    private final LifecycleService lifecycleService = Mockito.mock(LifecycleService.class);

    /** Optional eula returned by {@link EulaLauncher#eulaLaunch}. */
    private Optional<Eula> eula = Optional.empty();

    /**
     * The launch target of the servers of these tests. Like the real one it gives a server without
     * a Java version one, but it reports what it is asked to launch instead of starting a server.
     */
    private final class RecordingTarget extends FakeLaunchTarget {
        private @Nullable ProcessLauncher processLauncher;

        private RecordingTarget() {
            super(VersionHandle.SERVER);
            withSide(Side.SERVER);
        }

        @Override
        public Profile resolve(Profile profile, TypedMap options) {
            return profile.javaVersion() == null ? profile.withJavaVersion(21) : profile;
        }

        @Override
        public ProcessLauncher launcher(Profile profile, TypedMap options) {
            launched().add(profile);
            if (processLauncher != null) {
                return processLauncher;
            }

            throw new Launched(profile);
        }
    }

    private final RecordingTarget target = new RecordingTarget();
    private LaunchTargetService launchTargetService;

    /** A launcher whose eula launcher reports what it was asked to launch. */
    private ServerLauncher recordingLauncher() {
        ServerLauncher launcher = Mockito.mock(ServerLauncher.class);

        EulaLauncher eulaLauncher = Mockito.mock(EulaLauncher.class);
        Mockito.when(eulaLauncher.eulaLaunch(Mockito.any())).thenAnswer(invocation -> {
            eulaLaunches.add(invocation.getArgument(0));
            return new EulaLauncher.Result(eula.isPresent() ? EulaStatus.EXISTS : EulaStatus.NONE, eula);
        });
        Mockito.when(launcher.getEulaLauncher()).thenReturn(eulaLauncher);

        return launcher;
    }

    @BeforeEach
    void setup() {
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));
        profileService = new FakeProfileService(root.resolve("profiles"));
        serverService = new FakeServerService(profileService);
        launchTargetService = new TestLaunchTargets(profileService).with(target).service();
        profileResolver = new ProfileResolver(profileService, launchTargetService);
        serverLauncher = recordingLauncher();
    }

    private Profile server(String name) {
        Path dir = createDir(root.resolve(name));
        return profileService.save(new Profile(name, VersionArg.parse("server", "vanilla", "1.21.1"), dir));
    }

    private Eula eulaFile(Profile profile, String content) {
        Path file = profile.path().resolve("eula.txt");
        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new Eula(file);
    }

    // ------------------------------------------------------- LaunchCommand

    private ServerLaunchCommand launch() {
        return launch(serverService);
    }

    private ServerLaunchCommand launch(ServerService serverService) {
        return new ServerLaunchCommand(
            lifecycleService, profileResolver, platformService, serverLauncher, serverService,
            line -> line.split(" "), console, launchTargetService, McFilesService.of(TestFiles.mcFiles(root))
        );
    }

    @Test
    void launchWithoutNameThrows() {
        assertThrows(IllegalArgumentException.class, launch()::call);
    }

    @Test
    void launchUnknownServerThrows() {
        ServerLaunchCommand command = launch();
        command.setName("nope");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("Failed to find server/profile with name nope"));
    }

    @Test
    void launchClientProfileThrows() {
        Profile client = profileService.createDefault("client", VersionArg.parse("vanilla", "1.21.1"));
        ServerService clientReturning = new FakeServerService(profileService) {
            @Override
            public Optional<Profile> getServer(String name) {
                return Optional.of(client);
            }
        };

        ServerLaunchCommand command = launch(clientReturning);
        command.setName("client");
        assertThrows(IllegalStateException.class, command::call);
    }

    @Test
    void launchResolvesAndSavesJavaVersion() {
        server("srv");

        ServerLaunchCommand command = launch();
        command.setName("srv");
        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(21, launched.profile.javaVersion());
        assertEquals(21, profileService.getProfile("srv").orElseThrow().javaVersion());
    }

    @Test
    void launchKeepsExistingJavaVersion() {
        profileService.save(server("srv").withJavaVersion(17));

        ServerLaunchCommand command = launch();
        command.setName("srv");
        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(17, launched.profile.javaVersion());
    }

    @Test
    void launchWithEulaFlagAcceptsEula() {
        Profile server = server("srv");
        eula = Optional.of(eulaFile(server, "eula=false"));

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setAcceptEula(true);
        assertThrows(Launched.class, command::call);

        assertEquals(1, eulaLaunches.size());
        assertTrue(eula.orElseThrow().isAccepted());
    }

    @Test
    void theEulaStatusIsSavedWithoutTheOptionsOfTheLaunch() {
        Profile server = profileService.save(server("srv").withVmArgs(List.of("-Xms1G")).withGameArgs(List.of("nogui")));
        eula = Optional.of(eulaFile(server, "eula=false"));

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setAcceptEula(true);
        command.setJvmArgs("-Xmx8G -Dproperty=value");
        command.setGameArgs("--port 25566");
        Launched launched = assertThrows(Launched.class, command::call);

        // the server is launched with the options of this launch, also to create its eula.txt ...
        assertEquals(List.of("-Xms1G", "-Xmx8G"), launched.profile.vmArgs());
        assertEquals(List.of("-Xms1G", "-Xmx8G"), eulaLaunches.getFirst().vmArgs());
        // ... but what is saved is the stored profile, with what has been found out about it
        Profile stored = profileService.getProfile("srv").orElseThrow();
        assertEquals(EulaStatus.EXISTS, stored.eulaStatus());
        assertEquals(21, stored.javaVersion());
        assertEquals(List.of("-Xms1G"), stored.vmArgs());
        assertEquals(List.of("nogui"), stored.gameArgs());
        assertFalse(stored.systemProperties().containsKey("property"));
        assertEquals(server.withEulaStatus(EulaStatus.EXISTS).withJavaVersion(21), stored);
    }

    @Test
    void theRealEulaLauncherDoesNotSaveTheOptionsOfTheLaunchEither() {
        Profile server = server("srv");
        Eula file = eulaFile(server, "eula=false");
        Mockito.when(serverLauncher.getEulaLauncher()).thenReturn(new EulaLauncher(serverLauncher));

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setAcceptEula(true);
        command.setJvmArgs("-Xmx8G");
        Launched launched = assertThrows(Launched.class, command::call);

        assertTrue(file.isAccepted());
        assertEquals(List.of("-Xmx8G"), launched.profile.vmArgs());
        assertEquals(server.withEulaStatus(EulaStatus.EXISTS).withJavaVersion(21), profileService.getProfile("srv").orElseThrow());
    }

    @Test
    void aServerWithoutAEulaIsRememberedAsSuch() {
        Profile server = server("srv");

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setAcceptEula(true);
        command.setJvmArgs("-Xmx8G");
        assertThrows(Launched.class, command::call);

        assertEquals(server.withEulaStatus(EulaStatus.NONE).withJavaVersion(21), profileService.getProfile("srv").orElseThrow());
    }

    @Test
    void withATargetTheParametersAreNotANameButWhatToLaunch() {
        Path dir = createDir(root.resolve("lobby"));
        Profile lobby = profileService.save(new Profile("lobby", Optional.of(Side.SERVER), target.handle("paper"), dir));

        ServerLaunchCommand command = launch();
        command.setTarget(VersionHandle.SERVER);
        command.setName("paper");
        Launched launched = assertThrows(Launched.class, command::call);

        // the stored server for these arguments
        assertEquals("lobby", launched.profile.name());
        assertEquals(lobby.withJavaVersion(21), profileService.getProfile("lobby").orElseThrow());
    }

    @Test
    void aNameAndATargetExcludeEachOther() {
        Path dir = createDir(root.resolve("lobby"));
        Profile lobby = profileService.save(new Profile("lobby", Optional.of(Side.SERVER), target.handle("paper"), dir));

        // "lobby" is not the name of the server to launch with something else,
        // it is the first of the arguments for the launch target, and there is no server for them
        ServerLaunchCommand command = launch();
        command.setTarget(VersionHandle.SERVER);
        command.setName("lobby");
        command.setTargetArgs(List.of("purpur", "1.21.4"));
        command.setAcceptEula(true);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);

        assertTrue(e.getMessage().contains("lobby purpur 1.21.4"), e.getMessage());
        assertEquals(List.of("lobby", "purpur", "1.21.4"), target.parsed().getLast().args());
        assertEquals(List.of(), target.launched());
        assertEquals(List.of(), eulaLaunches);
        assertEquals(lobby, profileService.getProfile("lobby").orElseThrow());
    }

    @Test
    void launchWithoutEulaFlagDoesNotTouchEula() {
        server("srv");

        ServerLaunchCommand command = launch();
        command.setName("srv");
        assertThrows(Launched.class, command::call);

        assertEquals(List.of(), eulaLaunches);
    }

    @Test
    void launchReturnsTheStoredServer() {
        Profile server = server("srv");

        ServerLaunchCommand command = launch();
        command.setName("srv");

        assertEquals(server, command.getProfile());
    }

    @Test
    void launchAppendsJvmAndGameArgs() {
        profileService.save(server("srv").withVmArgs(List.of("-Xms1G")).withGameArgs(List.of("--port", "25565")));

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setJvmArgs("-Xmx2G -Dproperty=value");
        command.setGameArgs("nogui");
        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("-Xms1G", "-Xmx2G"), launched.profile.vmArgs());
        assertEquals("value", launched.profile.systemProperties().get("property"));
        assertEquals(List.of("--port", "25565", "nogui"), launched.profile.gameArgs());
    }

    @Test
    void launchSupportsServersWithoutGameArgs() {
        profileService.save(server("srv").withGameArgs(null));

        ServerLaunchCommand command = launch();
        command.setName("srv");
        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of(), launched.profile.gameArgs());
    }

    @Test
    void launchedProcessIsHandledByTheLifecycle() {
        server("srv");
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        ProcessLifecycle lifecycle = Mockito.mock(ProcessLifecycle.class);
        target.processLauncher = processLauncher;
        Mockito.when(lifecycleService.wrap(processLauncher, 2)).thenReturn(lifecycle);
        Mockito.when(lifecycle.call()).thenReturn(5);

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setRetries(2);

        assertEquals(5, command.call());
        command.run();
        Mockito.verify(lifecycle, Mockito.times(2)).call();
    }

    @Test
    void launchNotifiesTheLifecycleBeforeLaunching() {
        server("srv");
        ServerLaunchCommand command = launch();
        command.setName("srv");
        assertThrows(Launched.class, command::call);
        Mockito.verify(lifecycleService).beforeLaunch(Mockito.argThat(profile -> profile.name().equals("srv")), Mockito.eq(true));

        command.setNoBackup(true);
        assertThrows(Launched.class, command::call);
        Mockito.verify(lifecycleService).beforeLaunch(Mockito.argThat(profile -> profile.name().equals("srv")), Mockito.eq(false));
    }

    @Test
    void dryRunInstallsButDoesNotLaunch() {
        server("srv");
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        target.processLauncher = processLauncher;

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setAcceptEula(true);
        command.setDryRun(true);

        assertEquals(0, command.call());
        // everything is resolved and the process is built, it is just not started
        assertEquals(List.of("srv"), target.launched().stream().map(Profile::name).toList());
        Mockito.verify(lifecycleService, Mockito.never()).wrap(Mockito.any(), Mockito.anyInt());
        Mockito.verify(lifecycleService, Mockito.never()).beforeLaunch(Mockito.any(), Mockito.anyBoolean());
        assertEquals(List.of(), eulaLaunches);
    }

    @Test
    void dryRunResolvesTheJavaRuntime() {
        server("srv");
        JavaProcessBuilder javaProcessBuilder = Mockito.mock(JavaProcessBuilder.class);
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        Mockito.when(processLauncher.getJavaProcessBuilder()).thenReturn(Optional.of(javaProcessBuilder));
        target.processLauncher = processLauncher;

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setDryRun(true);

        assertEquals(0, command.call());
        Mockito.verify(javaProcessBuilder).resolveJava();
        Mockito.verify(javaProcessBuilder, Mockito.never()).start();
    }

    @Test
    void dryRunFailsIfNoJavaRuntimeIsFound() {
        server("srv");
        JavaProcessBuilder javaProcessBuilder = Mockito.mock(JavaProcessBuilder.class);
        Mockito.when(javaProcessBuilder.resolveJava()).thenThrow(new JavaProcessException("Failed to find Java 21"));
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        Mockito.when(processLauncher.getJavaProcessBuilder()).thenReturn(Optional.of(javaProcessBuilder));
        target.processLauncher = processLauncher;

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setDryRun(true);

        assertThrows(JavaProcessException.class, command::call);
        Mockito.verify(lifecycleService, Mockito.never()).wrap(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void dryRunFailsIfInstallingFails() {
        server("srv");
        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setDryRun(true);

        assertThrows(Launched.class, command::call);
        Mockito.verify(lifecycleService, Mockito.never()).wrap(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void onlyALaunchThatIsNotADryRunIsFinal() {
        ServerLaunchCommand command = launch();
        assertTrue(command.isFinal());

        command.setDryRun(true);
        assertFalse(command.isFinal());
    }

    // --------------------------------------------------------- EulaCommand

    private EulaCommand eulaCommand() {
        return new EulaCommand(profileResolver, serverLauncher, serverService, console);
    }

    @Test
    void eulaWithoutNameThrows() {
        EulaCommand.ReadCommand read = new EulaCommand.ReadCommand();
        read.setCtx(eulaCommand());
        assertThrows(IllegalArgumentException.class, read::run);
    }

    @Test
    void eulaReadPrintsContent() {
        Profile server = server("srv");
        eula = Optional.of(eulaFile(server, "eula=false"));

        EulaCommand.ReadCommand read = new EulaCommand.ReadCommand();
        read.setCtx(eulaCommand());
        read.setName("srv");
        read.run();

        assertTrue(console.output().contains("eula=false"));
    }

    @Test
    void eulaReadWithoutEula() {
        server("srv");

        EulaCommand.ReadCommand read = new EulaCommand.ReadCommand();
        read.setCtx(eulaCommand());
        read.setName("srv");
        read.run();

        assertTrue(console.output().contains("does not have a EULA"));
    }

    @Test
    void eulaAcceptWritesTrue() {
        Profile server = server("srv");
        Eula file = eulaFile(server, "#comment\neula=false");
        eula = Optional.of(file);

        EulaCommand.AcceptCommand accept = new EulaCommand.AcceptCommand();
        accept.setCtx(eulaCommand());
        accept.setName("srv");
        accept.run();

        assertTrue(file.isAccepted());
        assertTrue(console.output().contains("Accepted the EULA of srv"));
    }

    @Test
    void eulaAcceptWithoutEula() {
        server("srv");

        EulaCommand.AcceptCommand accept = new EulaCommand.AcceptCommand();
        accept.setCtx(eulaCommand());
        accept.setName("srv");
        accept.run();

        assertTrue(console.output().contains("does not have a EULA"));
    }

    private Path createDir(Path path) {
        try {
            return Files.createDirectories(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
