package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.ProcessLifecycle;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.Eula;
import io.github.headlesshq.headlessmc.launcher.server.EulaLauncher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProfileLaunchCommandTest {
    private static final class Launched extends RuntimeException {
        private final transient Profile profile;

        Launched(Profile profile) {
            this.profile = profile;
        }
    }

    private static ProcessLauncher launched(InvocationOnMock invocation) {
        throw new Launched(invocation.getArgument(0));
    }

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final ClientLauncher clientLauncher = Mockito.mock(ClientLauncher.class);
    private final ServerLauncher serverLauncher = Mockito.mock(ServerLauncher.class);
    private final ServerService serverService = Mockito.mock(ServerService.class);
    private final LastUsedAccountService lastUsedAccountService = Mockito.mock(LastUsedAccountService.class);
    private final XvfbService xvfbService = Mockito.mock(XvfbService.class);
    private final AuthService authService = Mockito.mock(AuthService.class);
    private final Account account = new Account("microsoft", "player", "uuid", "token", Account.TYPE_MSA, "xuid");
    private final LifecycleService lifecycleService = Mockito.mock(LifecycleService.class);
    private final EulaLauncher eulaLauncher = Mockito.mock(EulaLauncher.class);
    private final List<Profile> eulaLaunches = new ArrayList<>();

    private FakePlatformService platformService;
    private FakeProfileService profileService;

    @BeforeEach
    void setup() {
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));
        profileService = new FakeProfileService(root);

        Mockito.when(clientLauncher.launcher(Mockito.any(), Mockito.any())).thenAnswer(ProfileLaunchCommandTest::launched);
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.of(account));
        Mockito.when(authService.refresh(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(serverLauncher.launcher(Mockito.any())).thenAnswer(ProfileLaunchCommandTest::launched);
        Mockito.when(serverLauncher.findJavaVersion(Mockito.any())).thenAnswer(invocation -> {
            Profile profile = invocation.getArgument(0);
            return profile.javaVersion() == null ? profile.withJavaVersion(21) : profile;
        });
        Mockito.when(serverService.save(Mockito.any())).thenAnswer(invocation -> profileService.save(invocation.getArgument(0)));
        Mockito.when(serverLauncher.getEulaLauncher()).thenReturn(eulaLauncher);
        Mockito.when(eulaLauncher.eulaLaunch(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            eulaLaunches.add(invocation.getArgument(0));
            return Optional.<Eula>empty();
        });
    }

    private ProfileLaunchCommand command() {
        return new ProfileLaunchCommand(
            lifecycleService, new ProfileResolver(platformService, profileService), platformService,
            serverLauncher, serverService, clientLauncher, line -> line.split(" "), console,
            TestFiles.mcFiles(root), lastUsedAccountService, xvfbService, authService, profileService
        );
    }

    private ProfileLaunchCommand command(String name) {
        ProfileLaunchCommand command = command();
        command.setName(name);
        return command;
    }

    private Profile client(String name) {
        return profileService.save(new Profile(name, VersionArg.parse("vanilla", "1.21.1"), root.resolve(name)));
    }

    private Profile server(String name) {
        return profileService.save(new Profile(name, VersionArg.parse("server", "vanilla", "1.21.1"), root.resolve(name)));
    }

    @Test
    void launchWithoutNameThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command()::call);
        assertTrue(e.getMessage().contains("specify the name"));
        assertThrows(IllegalArgumentException.class, command()::run);
    }

    @Test
    void launchUnknownProfileThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command("nope")::call);
        assertTrue(e.getMessage().contains("Failed to find profile with name nope"));
    }

    @Test
    void getProfileReturnsTheStoredProfile() {
        Profile profile = client("main");

        assertEquals(profile, command("main").getProfile());
    }

    @Test
    void launchesAClientProfileWithTheClientLauncher() {
        client("main");

        Launched launched = assertThrows(Launched.class, command("main")::call);

        assertEquals("main", launched.profile.name());
        assertEquals(List.of("log4j"), launched.profile.patchers());
        Mockito.verify(serverLauncher, Mockito.never()).launcher(Mockito.any());
    }

    @Test
    void headlessAddsTheLwjglPatchers() {
        client("main");
        ProfileLaunchCommand command = command("main");
        command.setHeadless(true);

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("log4j", "lwjgl", "paulscode"), launched.profile.patchers());
    }

    @Test
    void patchersAreMergedLowerCasedAndDeduplicated() {
        profileService.save(client("main").withPatchers(List.of("log4j", "custom")));
        ProfileLaunchCommand command = command("main");
        command.setPatchers(new ArrayList<>(List.of("Custom", "Other")));

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("custom", "other", "log4j"), launched.profile.patchers());
    }

    @Test
    void jvmAndGameArgsAreAppendedToTheProfile() {
        profileService.save(client("main").withVmArgs(List.of("-Xms1G")).withGameArgs(List.of("--demo")));
        ProfileLaunchCommand command = command("main");
        command.setJvmArgs("-Xmx2G -Dproperty=value");
        command.setGameArgs("--quickPlayRealms 1234");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("-Xms1G", "-Xmx2G"), launched.profile.vmArgs());
        assertEquals("value", launched.profile.systemProperties().get("property"));
        assertEquals(List.of("--demo", "--quickPlayRealms", "1234"), launched.profile.gameArgs());
    }

    @Test
    void resolutionAndServerBecomeLaunchOptions() {
        client("main");
        ProfileLaunchCommand command = command("main");
        command.setResolution("1024x768");
        command.setServer("example.com");

        Launched launched = assertThrows(Launched.class, command::call);

        LaunchOptions options = launched.profile.options();
        assertEquals(Optional.of(new LaunchOptions.Resolution(1024, 768)), options.resolution());
        assertEquals(Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, "example.com")), options.join());
        assertTrue(options.quickPlayPath().orElseThrow().contains("quickPlay"));
    }

    @Test
    void profileOptionsAreUsedAsDefaults() {
        profileService.save(client("main").withOptions(
            new LaunchOptions(Optional.of(new LaunchOptions.Resolution(800, 600)), Optional.empty(), Optional.empty(), false)
        ));

        Launched launched = assertThrows(Launched.class, command("main")::call);

        assertEquals(Optional.of(new LaunchOptions.Resolution(800, 600)), launched.profile.options().resolution());
    }

    @Test
    void serverProfilesAreLaunchedWithTheServerLauncher() {
        server("srv");

        Launched launched = assertThrows(Launched.class, command("srv")::call);

        assertEquals("srv", launched.profile.name());
        assertEquals(List.of(), launched.profile.patchers());
        assertEquals(21, launched.profile.javaVersion());
        assertEquals(21, profileService.getProfile("srv").orElseThrow().javaVersion());
        Mockito.verify(clientLauncher, Mockito.never()).launcher(Mockito.any(), Mockito.any());
    }

    @Test
    void serverProfilesKeepTheirJavaVersion() {
        profileService.save(server("srv").withJavaVersion(17));

        Launched launched = assertThrows(Launched.class, command("srv")::call);

        assertEquals(17, launched.profile.javaVersion());
        Mockito.verify(serverService, Mockito.never()).save(Mockito.any());
    }

    @Test
    void serverProfilesAcceptTheEulaWhenRequested() {
        server("srv");
        ProfileLaunchCommand command = command("srv");
        command.setAcceptEula(true);

        assertThrows(Launched.class, command::call);

        assertEquals(1, eulaLaunches.size());
        assertEquals("srv", eulaLaunches.getFirst().name());
    }

    @Test
    void serverProfilesDoNotTouchTheEulaByDefault() {
        server("srv");

        assertThrows(Launched.class, command("srv")::call);

        assertEquals(List.of(), eulaLaunches);
    }

    @Test
    void clientOnlyOptionsAreIgnoredForServers() {
        server("srv");
        ProfileLaunchCommand command = command("srv");
        command.setResolution("1024x768");

        assertThrows(Launched.class, command::call);
    }

    @Test
    void launchedProcessIsHandledByTheLifecycle() throws Exception {
        client("main");
        ProcessLauncher processLauncher = Mockito.mock(ProcessLauncher.class);
        ProcessLifecycle lifecycle = Mockito.mock(ProcessLifecycle.class);
        Mockito.doReturn(processLauncher).when(clientLauncher).launcher(Mockito.any(), Mockito.any());
        Mockito.when(lifecycleService.wrap(processLauncher, 3)).thenReturn(lifecycle);
        Mockito.when(lifecycle.call()).thenReturn(7);

        ProfileLaunchCommand command = command("main");
        command.setRetries(3);

        assertEquals(7, command.call());
        Mockito.verify(lifecycleService).wrap(processLauncher, 3);
    }

    @Test
    void clientsLaunchWithTheRefreshedLastUsedAccount() {
        client("main");
        Account refreshed = new Account("microsoft", "player", "uuid", "refreshed", Account.TYPE_MSA, "xuid");
        Mockito.when(authService.refresh(account)).thenReturn(refreshed);

        assertThrows(Launched.class, command("main")::call);

        Mockito.verify(clientLauncher).launcher(Mockito.any(), Mockito.eq(refreshed));
    }

    @Test
    void launchingAClientWithoutAnAccountThrows() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());

        IllegalStateException e = assertThrows(IllegalStateException.class, command("main")::call);
        assertTrue(e.getMessage().contains("login"));
    }

    @Test
    void offlineUsesTheLastUsedAccountWithoutRefreshing() {
        client("main");
        ProfileLaunchCommand command = command("main");
        command.setOffline(true);

        assertThrows(Launched.class, command::call);

        Mockito.verify(clientLauncher).launcher(Mockito.any(), Mockito.eq(account));
        Mockito.verify(authService, Mockito.never()).refresh(Mockito.any());
    }

    @Test
    void offlineWithoutAnAccountUsesTheDefaultOfflineAccount() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());
        ProfileLaunchCommand command = command("main");
        command.setOffline(true);

        assertThrows(Launched.class, command::call);

        Mockito.verify(clientLauncher).launcher(Mockito.any(), Mockito.eq(Account.defaultOfflineAccount()));
    }

    @Test
    void offlineAccountsWithoutTheGameAreForcedHeadless() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());
        ProfileLaunchCommand command = command("main");
        command.setOffline(true);

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("log4j", "lwjgl", "paulscode"), launched.profile.patchers());
    }

    @Test
    void offlineAccountsRunningWithXvfbAreNotForcedHeadless() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());
        Mockito.when(xvfbService.isRunningWithXvfb()).thenReturn(true);
        ProfileLaunchCommand command = command("main");
        command.setOffline(true);

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("log4j"), launched.profile.patchers());
    }

    @Test
    void offlineAccountsThatOwnTheGameAreNotForcedHeadless() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());
        AuthProvider microsoft = Mockito.mock(AuthProvider.class);
        Mockito.when(microsoft.getName()).thenReturn("microsoft");
        Mockito.when(microsoft.getAccounts()).thenReturn(List.of(account));
        Mockito.when(authService.getProviders()).thenReturn(List.of(microsoft));
        ProfileLaunchCommand command = command("main");
        command.setOffline(true);

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("log4j"), launched.profile.patchers());
    }

    @Test
    void serversDoNotRequireAnAccount() {
        server("srv");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());

        assertThrows(Launched.class, command("srv")::call);

        Mockito.verifyNoInteractions(lastUsedAccountService);
    }

}
