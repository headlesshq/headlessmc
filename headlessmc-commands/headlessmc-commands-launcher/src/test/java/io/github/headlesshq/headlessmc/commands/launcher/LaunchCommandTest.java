package io.github.headlesshq.headlessmc.commands.launcher;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.commands.profile.XvfbService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LaunchCommandTest {
    /** Thrown instead of actually launching, carrying the profile the command built. */
    private static final class Launched extends RuntimeException {
        private final transient Profile profile;

        Launched(Profile profile) {
            this.profile = profile;
        }
    }

    private static Profile launched(InvocationOnMock invocation) {
        throw new Launched(invocation.getArgument(0));
    }

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();

    private final ClientLauncher clientLauncher = Mockito.mock(ClientLauncher.class);
    private final ServerLauncher serverLauncher = Mockito.mock(ServerLauncher.class);
    private final LifecycleService lifecycleService = Mockito.mock(LifecycleService.class);
    private final ServerService serverService = Mockito.mock(ServerService.class);
    private final LastUsedAccountService lastUsedAccountService = Mockito.mock(LastUsedAccountService.class);
    private final XvfbService xvfbService = Mockito.mock(XvfbService.class);
    private final AuthService authService = Mockito.mock(AuthService.class);
    private final Account account = new Account("microsoft", "player", "uuid", "token", Account.TYPE_MSA, "xuid");

    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private ProfileResolver profileResolver;
    private McFiles mcFiles;

    @BeforeEach
    void setup() {
        // the launchers report the profile the command built instead of launching it
        Mockito.when(clientLauncher.launcher(Mockito.any(), Mockito.any())).thenAnswer(LaunchCommandTest::launched);
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.of(account));
        Mockito.when(authService.refresh(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(serverLauncher.launcher(Mockito.any())).thenAnswer(LaunchCommandTest::launched);
        Mockito.when(serverLauncher.findJavaVersion(Mockito.any())).thenAnswer(invocation -> {
            Profile profile = invocation.getArgument(0);
            return profile.javaVersion() == null ? profile.withJavaVersion(21) : profile;
        });
        Mockito.when(serverService.save(Mockito.any())).thenAnswer(invocation -> profileService.save(invocation.getArgument(0)));

        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));
        profileService = new FakeProfileService(root);
        profileResolver = new ProfileResolver(platformService, profileService);
        mcFiles = TestFiles.mcFiles(root);
    }

    private LaunchCommand command() {
        return new LaunchCommand(
            lifecycleService, profileResolver, platformService, serverLauncher, serverService, clientLauncher,
            line -> line.split(" "), console, mcFiles, lastUsedAccountService, xvfbService, authService
        );
    }

    private Profile client(String name) {
        return profileService.save(
            new Profile(name, VersionArg.parse("vanilla", "1.21.1"), root.resolve(name))
        );
    }

    private Profile server(String name) {
        return profileService.save(
            new Profile(name, VersionArg.parse("server", "vanilla", "1.21.1"), root.resolve(name))
        );
    }

    @Test
    void emptyVersionArgThrows() {
        assertThrows(IllegalArgumentException.class, command()::call);
        assertThrows(IllegalArgumentException.class, command()::run);
    }

    @Test
    void versionArgIsExposedForCompletions() {
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));

        assertEquals(List.of("main"), command.getVersionArg());
    }

    @Test
    void launchesAClientProfile() {
        client("main");
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals("main", launched.profile.name());
        assertTrue(launched.profile.patchers().contains("log4j"));
    }

    @Test
    void headlessAddsTheLwjglPatchers() {
        client("main");
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));
        command.setHeadless(true);

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("log4j", "lwjgl", "paulscode"), launched.profile.patchers());
    }

    @Test
    void patchersAreLowerCasedAndDeduplicated() {
        profileService.save(client("main").withPatchers(List.of("log4j", "custom")));
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));
        command.setPatchers(new java.util.ArrayList<>(List.of("Custom")));

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("custom", "log4j"), launched.profile.patchers());
    }

    @Test
    void jvmArgsAreSplitIntoArgsAndSystemProperties() {
        profileService.save(client("main").withVmArgs(List.of("-Xms1G")));
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));
        command.setJvmArgs("-Xmx2G -Dproperty=value");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("-Xms1G", "-Xmx2G"), launched.profile.vmArgs());
        assertEquals("value", launched.profile.systemProperties().get("property"));
    }

    @Test
    void gameArgsAreAppendedToTheProfileArgs() {
        profileService.save(client("main").withGameArgs(List.of("--demo")));
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));
        command.setGameArgs("--quickPlayRealms 1234");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("--demo", "--quickPlayRealms", "1234"), launched.profile.gameArgs());
    }

    @Test
    void profilesWithoutGameArgsAreSupported() {
        profileService.save(client("main").withGameArgs(null));
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of(), launched.profile.gameArgs());
    }

    @Test
    void resolutionAndServerBecomeLaunchOptions() {
        client("main");
        LaunchCommand command = command();
        command.setVersionArg(List.of("main"));
        command.setResolution("1024x768");
        command.setServer("example.com");

        Launched launched = assertThrows(Launched.class, command::call);

        LaunchOptions options = launched.profile.options();
        assertEquals(Optional.of(new LaunchOptions.Resolution(1024, 768)), options.resolution());
        assertEquals(
            Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, "example.com")), options.join()
        );
        assertTrue(options.quickPlayPath().orElseThrow().contains("quickPlay"));
    }

    @Test
    void serverProfilesAreLaunchedWithTheServerLauncher() {
        server("srv");
        LaunchCommand command = command();
        command.setVersionArg(List.of("srv"));

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals("srv", launched.profile.name());
        // patchers are client-only
        assertEquals(List.of(), launched.profile.patchers());
    }

    @Test
    void clientOnlyOptionsAreIgnoredForServers() {
        server("srv");
        LaunchCommand command = command();
        command.setVersionArg(List.of("srv"));
        command.setResolution("1024x768");

        assertThrows(Launched.class, command::call);
    }

}
