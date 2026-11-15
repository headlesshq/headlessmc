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
import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.FakeLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
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

    /** What the launch targets of these tests are asked to launch. */
    private interface Launches {
        ProcessLauncher client(Profile profile, Account account);

        ProcessLauncher server(Profile profile);
    }

    private final Launches launches = Mockito.mock(Launches.class);
    /** Stands in for the launch target of clients: launches with the account the command chose. */
    private final FakeLaunchTarget clientTarget = new FakeLaunchTarget(VersionHandle.VERSION) {
        @Override
        public ProcessLauncher launcher(Profile profile, TypedMap options) {
            return launches.client(profile, options.get(ClientLauncher.ACCOUNT));
        }
    }.withSide(Side.CLIENT);
    /** Stands in for the launch target of servers: like the real one it gives a server a Java version. */
    private final FakeLaunchTarget serverTarget = new FakeLaunchTarget(VersionHandle.SERVER) {
        @Override
        public Profile resolve(Profile profile, TypedMap options) {
            return profile.javaVersion() == null ? profile.withJavaVersion(21) : profile;
        }

        @Override
        public ProcessLauncher launcher(Profile profile, TypedMap options) {
            return launches.server(profile);
        }
    }.withSide(Side.SERVER);

    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private LaunchTargetService launchTargetService;

    @BeforeEach
    void setup() {
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));
        profileService = new FakeProfileService(root);
        launchTargetService = new TestLaunchTargets(profileService).with(clientTarget, serverTarget).service();

        Mockito.when(launches.client(Mockito.any(), Mockito.any())).thenAnswer(ProfileLaunchCommandTest::launched);
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.of(account));
        Mockito.when(authService.refresh(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(launches.server(Mockito.any())).thenAnswer(ProfileLaunchCommandTest::launched);
        Mockito.when(serverService.save(Mockito.any())).thenAnswer(invocation -> profileService.save(invocation.getArgument(0)));
        Mockito.when(serverLauncher.getEulaLauncher()).thenReturn(eulaLauncher);
        Mockito.when(eulaLauncher.eulaLaunch(Mockito.any())).thenAnswer(invocation -> {
            eulaLaunches.add(invocation.getArgument(0));
            return new EulaLauncher.Result(EulaStatus.NONE, Optional.empty());
        });
    }

    private ProfileLaunchCommand command() {
        return new ProfileLaunchCommand(
            lifecycleService, new ProfileResolver(profileService, launchTargetService), platformService,
            serverLauncher, serverService, clientLauncher, line -> line.split(" "), console,
            launchTargetService, McFilesService.of(TestFiles.mcFiles(root)), lastUsedAccountService, xvfbService, authService, profileService
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
        Mockito.verify(launches, Mockito.never()).server(Mockito.any());
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
    void thePatchedLwjglCanBeSelectedForALaunch() {
        client("main");
        ProfileLaunchCommand command = command("main");
        command.setHeadless(true);
        command.setPatchers(new ArrayList<>(List.of("LWJGL-pre")));

        Launched launched = assertThrows(Launched.class, command::call);

        // both are selected, the patched LWJGL replaces the lwjgl patcher when the game is launched
        assertEquals(List.of("lwjgl-pre", "log4j", "lwjgl", "paulscode"), launched.profile.patchers());
    }

    @Test
    void thePatchedLwjglCanBeSelectedByTheProfile() {
        profileService.save(client("main").withPatchers(List.of("lwjgl-pre")));
        ProfileLaunchCommand command = command("main");
        command.setHeadless(true);

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("log4j", "lwjgl", "paulscode", "lwjgl-pre"), launched.profile.patchers());
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
        // the quick play log is written to the .minecraft directory the game is launched from,
        // which the command does not know, it is decided when the process is built
        assertEquals(Optional.empty(), options.quickPlayPath());
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
        Mockito.verify(launches, Mockito.never()).client(Mockito.any(), Mockito.any());
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
    void theEulaStatusIsSavedWithoutTheOptionsOfTheLaunch() {
        Profile server = server("srv");
        ProfileLaunchCommand command = command("srv");
        command.setAcceptEula(true);
        command.setJvmArgs("-Xmx8G");
        command.setGameArgs("--port 25566");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(List.of("-Xmx8G"), launched.profile.vmArgs());
        assertEquals(List.of("-Xmx8G"), eulaLaunches.getFirst().vmArgs());
        assertEquals(
            server.withJavaVersion(21).withEulaStatus(EulaStatus.NONE),
            profileService.getProfile("srv").orElseThrow()
        );
    }

    @Test
    void optionsThatDoNotFitTheProfileAreRejectedBeforeAnythingIsResolved() {
        Profile main = client("main");
        ProfileLaunchCommand command = command("main");
        command.setAcceptEula(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);

        assertTrue(e.getMessage().contains("--eula-accept"), e.getMessage());
        // rejected before the launch target installs anything, observers are told, or an account is refreshed
        assertEquals(List.of(), clientTarget.resolved());
        assertEquals(main, profileService.getProfile("main").orElseThrow());
        Mockito.verifyNoInteractions(lifecycleService, authService, launches);
    }

    @Test
    void clientOptionsAreRejectedForSomethingThatIsNeitherClientNorServerBeforeItIsResolved() {
        FakeLaunchTarget jar = new FakeLaunchTarget("jar").resolvingWith(profile -> profile.withJavaVersion(21));
        launchTargetService = new TestLaunchTargets(profileService).with(clientTarget, serverTarget, jar).service();
        Profile tool = profileService.save(new Profile("tool", Optional.empty(), jar.handle("tool.jar"), root.resolve("tool")));
        ProfileLaunchCommand command = command("tool");
        command.setHeadless(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);

        assertTrue(e.getMessage().contains("neither a client nor a server"), e.getMessage());
        assertEquals(List.of(), jar.resolved());
        // the Java version resolving finds out has not been saved for a launch that never happened
        assertEquals(tool, profileService.getProfile("tool").orElseThrow());
    }

    @Test
    void withATargetTheParametersAreNotANameButWhatToLaunch() {
        client("main");
        Profile other = profileService.save(
            new Profile("other", Optional.of(Side.CLIENT), clientTarget.handle("fabric"), root.resolve("other"))
        );

        ProfileLaunchCommand command = command("fabric");
        command.setTarget(VersionHandle.VERSION);
        Launched launched = assertThrows(Launched.class, command::call);

        // the stored profile for these arguments, like the launch command finds it
        assertEquals("other", launched.profile.name());
        assertEquals(other, profileService.getProfile("other").orElseThrow());
    }

    @Test
    void aNameAndATargetExcludeEachOther() {
        Profile main = client("main");
        ProfileLaunchCommand command = command("main");
        command.setTarget(VersionHandle.VERSION);
        command.setTargetArgs(List.of("fabric", "1.21.1"));

        Launched launched = assertThrows(Launched.class, command::call);

        // "main" is not the profile to launch with another target, it is the first argument for the target:
        // the stored profile main is neither launched nor changed
        assertEquals(List.of("main", "fabric", "1.21.1"), clientTarget.parsed().getLast().args());
        assertNotEquals(main.target(), launched.profile.target());
        assertEquals(main, profileService.getProfile("main").orElseThrow());
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
        Mockito.doReturn(processLauncher).when(launches).client(Mockito.any(), Mockito.any());
        Mockito.when(lifecycleService.wrap(processLauncher, 3)).thenReturn(lifecycle);
        Mockito.when(lifecycle.call()).thenReturn(7);

        ProfileLaunchCommand command = command("main");
        command.setRetries(3);

        assertEquals(7, command.call());
        Mockito.verify(lifecycleService).wrap(processLauncher, 3);
        Mockito.verify(lifecycleService).beforeLaunch(Mockito.any(), Mockito.eq(true));
    }

    @Test
    void noBackupSkipsAutomaticBackups() {
        client("main");
        ProfileLaunchCommand command = command("main");
        command.setNoBackup(true);

        assertThrows(Launched.class, command::call);
        Mockito.verify(lifecycleService).beforeLaunch(Mockito.any(), Mockito.eq(false));
    }

    @Test
    void clientsLaunchWithTheRefreshedLastUsedAccount() {
        client("main");
        Account refreshed = new Account("microsoft", "player", "uuid", "refreshed", Account.TYPE_MSA, "xuid");
        Mockito.when(authService.refresh(account)).thenReturn(refreshed);

        assertThrows(Launched.class, command("main")::call);

        Mockito.verify(launches).client(Mockito.any(), Mockito.eq(refreshed));
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

        Mockito.verify(launches).client(Mockito.any(), Mockito.eq(account));
        Mockito.verify(authService, Mockito.never()).refresh(Mockito.any());
    }

    @Test
    void offlineWithoutAnAccountUsesTheDefaultOfflineAccount() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());
        ProfileLaunchCommand command = command("main");
        command.setOffline(true);

        assertThrows(Launched.class, command::call);

        Mockito.verify(launches).client(Mockito.any(), Mockito.eq(Account.defaultOfflineAccount()));
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
    void yggdrasilAccountsAreForcedHeadless() {
        client("main");
        Account yggdrasil = new Account(
            "yggdrasil", "player", "uuid", "token", Account.TYPE_LEGACY, "");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.of(yggdrasil));

        Launched launched = assertThrows(Launched.class, () -> command("main").call());

        assertEquals(List.of("log4j", "lwjgl", "paulscode"), launched.profile.patchers());
    }

    @Test
    void registeringAYggdrasilAccountDoesNotCountAsOwningTheGame() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());
        AuthProvider yggdrasil = Mockito.mock(AuthProvider.class);
        Mockito.when(yggdrasil.getName()).thenReturn("yggdrasil");
        Mockito.when(yggdrasil.getAccounts()).thenReturn(List.of(account));
        Mockito.when(authService.getProviders()).thenReturn(List.of(yggdrasil));
        ProfileLaunchCommand command = command("main");
        command.setOffline(true);

        Launched launched = assertThrows(Launched.class, command::call);

        // a stored Yggdrasil account must not be mistaken for a purchased copy
        assertEquals(List.of("log4j", "lwjgl", "paulscode"), launched.profile.patchers());
    }

    @Test
    void serversDoNotRequireAnAccount() {
        server("srv");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());

        assertThrows(Launched.class, command("srv")::call);

        Mockito.verifyNoInteractions(lastUsedAccountService);
    }

}
