package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.ProcessLifecycle;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
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

    /** A launcher that reports what it was asked to launch instead of starting a server. */
    private ServerLauncher recordingLauncher() {
        ServerLauncher launcher = Mockito.mock(ServerLauncher.class);
        Mockito.when(launcher.findJavaVersion(Mockito.any())).thenAnswer(invocation -> {
            Profile profile = invocation.getArgument(0);
            return profile.javaVersion() == null ? profile.withJavaVersion(21) : profile;
        });
        Mockito.when(launcher.launcher(Mockito.any())).thenAnswer(invocation -> {
            throw new Launched(invocation.getArgument(0));
        });

        EulaLauncher eulaLauncher = Mockito.mock(EulaLauncher.class);
        Mockito.when(eulaLauncher.eulaLaunch(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            eulaLaunches.add(invocation.getArgument(0));
            return eula;
        });
        Mockito.when(launcher.getEulaLauncher()).thenReturn(eulaLauncher);

        return launcher;
    }

    @BeforeEach
    void setup() {
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));
        profileService = new FakeProfileService(root.resolve("profiles"));
        serverService = new FakeServerService(profileService);
        profileResolver = new ProfileResolver(platformService, profileService);
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
            line -> line.split(" "), console, TestFiles.mcFiles(root)
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
        Mockito.doReturn(processLauncher).when(serverLauncher).launcher(Mockito.any());
        Mockito.when(lifecycleService.wrap(processLauncher, 2)).thenReturn(lifecycle);
        Mockito.when(lifecycle.call()).thenReturn(5);

        ServerLaunchCommand command = launch();
        command.setName("srv");
        command.setRetries(2);

        assertEquals(5, command.call());
        command.run();
        Mockito.verify(lifecycle, Mockito.times(2)).call();
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
