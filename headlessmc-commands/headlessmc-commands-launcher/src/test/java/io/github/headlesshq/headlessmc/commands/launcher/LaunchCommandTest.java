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
import io.github.headlesshq.headlessmc.mods.packwiz.PackwizService;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;
import java.nio.file.Files;
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

        platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1"),
            FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.14"}),
            FakePlatform.create("quilt", new String[]{"1.21.1", "0.26.0"})
        );
        profileService = new FakeProfileService(root);
        profileResolver = new ProfileResolver(platformService, profileService);
        mcFiles = TestFiles.mcFiles(root);
    }

    private LaunchCommand command() {
        return new LaunchCommand(
            lifecycleService, profileResolver, platformService, serverLauncher, serverService, clientLauncher,
            line -> line.split(" "), console, mcFiles, lastUsedAccountService, xvfbService, authService,
            new PackwizService(platformService), profileService
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

    private Path packToml(String versions) throws IOException {
        Path packDir = Files.createDirectories(root.resolve("my pack"));
        return Files.writeString(packDir.resolve("pack.toml"), """
            name = "My Pack"
            pack-format = "packwiz:1.1.0"

            [index]
            file = "index.toml"
            hash-format = "sha256"
            hash = ""

            [versions]
            minecraft = "1.21.1"
            """ + versions);
    }

    private Path singleLoaderPack() throws IOException {
        return packToml("fabric = \"0.16.14\"\n");
    }

    private Path multiLoaderPack() throws IOException {
        return packToml("quilt = \"0.26.0\"\nfabric = \"0.16.14\"\n");
    }

    private LaunchCommand packwizCommand(Path pack, String... versionArg) {
        LaunchCommand command = command();
        command.setPackwiz(pack.toString());
        command.setVersionArg(List.of(versionArg));
        return command;
    }

    @Test
    void packwizWithoutSideThrows() throws IOException {
        LaunchCommand command = packwizCommand(singleLoaderPack());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("<side>"), e.getMessage());
        // spaces in the path are escaped in the suggested command
        assertTrue(e.getMessage().contains("my\\ pack"), e.getMessage());
    }

    @Test
    void packwizWithMultipleLoadersWithoutSideThrows() throws IOException {
        LaunchCommand command = packwizCommand(multiLoaderPack());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("<side> <loader>"), e.getMessage());
        assertTrue(e.getMessage().contains("quilt, fabric"), e.getMessage());
    }

    @Test
    void packwizLaunchesTheClient() throws IOException {
        Path pack = singleLoaderPack();
        LaunchCommand command = packwizCommand(pack, "client");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14").withSide(Side.CLIENT), launched.profile.version());
        assertEquals(Side.CLIENT, launched.profile.side());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizSideIsCaseInsensitive() throws IOException {
        LaunchCommand command = packwizCommand(singleLoaderPack(), "CLIENT");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(Side.CLIENT, launched.profile.side());
    }

    @Test
    void packwizLaunchesTheServer() throws IOException {
        Path pack = singleLoaderPack();
        LaunchCommand command = packwizCommand(pack, "server");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14").withSide(Side.SERVER), launched.profile.version());
        assertEquals(Side.SERVER, launched.profile.side());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
        // patchers are client-only
        assertEquals(List.of(), launched.profile.patchers());
    }

    @Test
    void packwizVanillaPackLaunchesVanilla() throws IOException {
        LaunchCommand command = packwizCommand(packToml(""), "client");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("vanilla", "1.21.1").withSide(Side.CLIENT), launched.profile.version());
    }

    @Test
    void packwizUsesAnExistingProfileInThePackDirectory() throws IOException {
        Path pack = multiLoaderPack();
        client("main");
        LaunchCommand command = packwizCommand(pack, "main");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals("main", launched.profile.name());
        assertEquals(VersionArg.parse("vanilla", "1.21.1"), launched.profile.version());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizWithUnknownSideThrows() throws IOException {
        LaunchCommand command = packwizCommand(singleLoaderPack(), "neither");

        assertThrows(IllegalArgumentException.class, command::call);
    }

    @Test
    void packwizWithMultipleLoadersAndOnlySideThrows() throws IOException {
        LaunchCommand command = packwizCommand(multiLoaderPack(), "client");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("loader"), e.getMessage());
        assertTrue(e.getMessage().contains("quilt, fabric"), e.getMessage());
    }

    @Test
    void packwizWithSideAndLoaderLaunchesThatLoader() throws IOException {
        Path pack = multiLoaderPack();
        LaunchCommand command = packwizCommand(pack, "server", "Fabric");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14").withSide(Side.SERVER), launched.profile.version());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizWithSideAndUnknownLoaderThrows() throws IOException {
        LaunchCommand command = packwizCommand(multiLoaderPack(), "client", "forge");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("Failed to find loader forge"), e.getMessage());
    }

    @Test
    void packwizWithAVersionArgResolvesThatVersion() throws IOException {
        Path pack = multiLoaderPack();
        LaunchCommand command = packwizCommand(pack, "quilt", "1.21.1");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("quilt", "1.21.1"), launched.profile.version());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizWithAFullVersionArgResolvesThatVersion() throws IOException {
        Path pack = multiLoaderPack();
        LaunchCommand command = packwizCommand(pack, "server", "fabric", "1.21.1");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("server", "fabric", "1.21.1"), launched.profile.version());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizAcceptsTheDirectoryOfThePack() throws IOException {
        Path pack = singleLoaderPack();
        LaunchCommand command = packwizCommand(pack.getParent(), "client");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14").withSide(Side.CLIENT), launched.profile.version());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizWithDirectoryWithoutPackTomlThrows() throws IOException {
        LaunchCommand command = packwizCommand(Files.createDirectories(root.resolve("empty")), "client");

        assertThrows(HeadlessMcException.class, command::call);
    }

    @Test
    void packwizWithMissingFileThrows() {
        LaunchCommand command = packwizCommand(root.resolve("missing").resolve("pack.toml"), "client");

        assertThrows(HeadlessMcException.class, command::call);
    }

}
