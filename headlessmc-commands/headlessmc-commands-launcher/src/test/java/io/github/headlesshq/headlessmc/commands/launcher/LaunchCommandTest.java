package io.github.headlesshq.headlessmc.commands.launcher;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.commands.profile.XvfbService;
import io.github.headlesshq.headlessmc.commands.util.LaunchTargetCompletions;
import io.github.headlesshq.headlessmc.commands.util.LaunchTargetNameCompletions;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.client.DefaultJvmArgsService;
import io.github.headlesshq.headlessmc.launcher.client.PackwizLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.client.VersionFileLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.FakeLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.FakeVersionLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.GameDirLayout;
import io.github.headlesshq.headlessmc.launcher.profile.GameDirService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.mods.packwiz.PackwizService;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.github.headlesshq.headlessmc.version.service.VersionJsonServiceImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LaunchCommandTest {
    /** Thrown instead of actually launching, carrying the profile the command built and the version.json to launch. */
    private static final class Launched extends RuntimeException {
        private final transient Profile profile;
        private final transient @Nullable Version version;

        Launched(Profile profile, @Nullable Version version) {
            this.profile = profile;
            this.version = version;
        }
    }

    /**
     * The launch target for the versions of one side. It parses arguments like the real targets,
     * and pins what it "resolved" like they do, but reports what it is asked to launch instead of launching it.
     */
    private final class RecordingVersionTarget extends FakeVersionLaunchTarget {
        private final List<@Nullable Account> accounts = new ArrayList<>();
        private @Nullable ProcessLauncher processLauncher;
        private @Nullable RuntimeException failure;

        private RecordingVersionTarget(Side side) {
            super(platformService, targets.jsonService(), side, root);
        }

        @Override
        public Profile resolve(Profile profile, TypedMap options) {
            VersionHandle handle = validate(profile);
            Profile result = profile;
            if (handle.currentTarget() == null) {
                result = result.withTarget(handle(handle.withCurrentTarget(root.resolve("pinned-" + profile.name()))));
            }

            // like the real target for servers
            return getSide().isServer() && result.javaVersion() == null ? result.withJavaVersion(21) : result;
        }

        @Override
        public ProcessLauncher launcher(Profile profile, TypedMap options) {
            accounts.add(options.get(ClientLauncher.ACCOUNT));
            if (failure != null) {
                throw failure;
            }

            if (processLauncher != null) {
                return processLauncher;
            }

            throw new Launched(profile, null);
        }
    }

    /** Parses a version.json that only contains the id of the version, and remembers the file it comes from. */
    private static final VersionParser PARSER = new VersionParser() {
        @Override
        public Version parse(Reader reader) {
            try (BufferedReader buffered = new BufferedReader(reader)) {
                return new FakeVersion(String.join("", buffered.readAllLines()).trim());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Version parse(Path jsonFile) {
            return ((FakeVersion) VersionParser.super.parse(jsonFile)).withFile(jsonFile);
        }
    };

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
    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final Account account = new Account("microsoft", "player", "uuid", "token", Account.TYPE_MSA, "xuid");

    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private ProfileResolver profileResolver;
    private McFiles mcFiles;
    private TestLaunchTargets targets;
    private LaunchTargetService launchTargetService;
    private RecordingVersionTarget clientTarget;
    private RecordingVersionTarget serverTarget;

    @BeforeEach
    void setup() {
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.of(account));
        Mockito.when(authService.refresh(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        // a version.json that is launched directly, the launcher reports it instead of launching it
        Mockito.when(clientLauncher.launcher(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(invocation -> {
                throw new Launched(invocation.getArgument(0), invocation.getArgument(2));
            });

        platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1"),
            FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.14"}),
            FakePlatform.create("quilt", new String[]{"1.21.1", "0.26.0"})
        );
        profileService = new FakeProfileService(root);
        mcFiles = TestFiles.mcFiles(root);
        McFilesService mcFilesService = McFilesService.of(mcFiles);
        VersionJsonService versionJsonService = new VersionJsonServiceImpl(PARSER, mcFilesService);
        GameDirService gameDirService = Mockito.mock(GameDirService.class);
        Mockito.when(gameDirService.getLayout()).thenReturn(GameDirLayout.HEADLESSMC);
        Mockito.when(gameDirService.getGameDir(Mockito.any(), Mockito.any()))
            .thenAnswer(invocation -> root.resolve(invocation.<VersionArg>getArgument(0).toString("-")));

        // the real targets for everything that is not a version, they all end up in one of the recording ones
        targets = new TestLaunchTargets(profileService);
        launchTargetService = targets.service();
        clientTarget = new RecordingVersionTarget(Side.CLIENT);
        serverTarget = new RecordingVersionTarget(Side.SERVER);
        targets.with(
            clientTarget,
            serverTarget,
            new VersionFileLaunchTarget(
                versionJsonService, mcFilesService, gameDirService, clientLauncher,
                Mockito.mock(DefaultJvmArgsService.class), fileService, targets.jsonService()
            ),
            new PackwizLaunchTarget(launchTargetService, new PackwizService(platformService), fileService, targets.jsonService()),
            targets.profileTarget()
        );

        profileResolver = new ProfileResolver(profileService, launchTargetService);
    }

    private LaunchCommand command() {
        return new LaunchCommand(
            lifecycleService, profileResolver, platformService, serverLauncher, serverService, clientLauncher,
            line -> line.split(" "), console, launchTargetService, McFilesService.of(mcFiles),
            lastUsedAccountService, xvfbService, authService
        );
    }

    private LaunchCommand command(String... args) {
        LaunchCommand command = command();
        command.setVersionArg(List.of(args));
        return command;
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

    private VersionArg version(Profile profile) {
        return launchTargetService.describe(profile).specified().orElseThrow();
    }

    /** Installs a version.json into the versions directory. */
    private Path install(String id) {
        try {
            Path dir = Files.createDirectories(mcFiles.getVersionsDir().resolve(id));
            return Files.writeString(dir.resolve(id + ".json"), id);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void emptyVersionArgThrows() {
        assertThrows(IllegalArgumentException.class, command()::call);
        assertThrows(IllegalArgumentException.class, command()::run);
    }

    @Test
    void theArgumentsAreExposedForCompletions() {
        LaunchCommand command = command("fabric", "1.");

        assertEquals(List.of("fabric", "1."), command.getTargetArgs());
        assertNull(command.getTarget());
        assertEquals(Side.BOTH, command.getSides());
        assertTrue(command.acceptsProfileNames());
    }

    // what is launched

    @Test
    void launchesAVersionJsonByItsPath() {
        Path json = install("Aeromania");

        Launched launched = assertThrows(Launched.class, command(json.toString())::call);

        // the version.json itself, a version installed by another launcher under a custom name
        assertNotNull(launched.version);
        assertEquals("Aeromania", launched.version.getId());
        assertEquals(Optional.of(json.toAbsolutePath().normalize()), launched.version.asFile());
        assertEquals(VersionFileLaunchTarget.NAME, launched.profile.target().name());
        assertEquals(Optional.of(Side.CLIENT), launched.profile.side());
        assertTrue(launched.profile.patchers().contains("log4j"));
        // nothing is stored for a single launch
        assertEquals(List.of(), profileService.getProfiles());
    }

    @Test
    void launchesAnInstalledVersionJsonByItsId() {
        // "Aeromania" is neither a profile nor a version, but the id of a version another launcher installed
        Path json = install("Aeromania");

        Launched launched = assertThrows(Launched.class, command("Aeromania")::call);

        assertNotNull(launched.version);
        assertEquals("Aeromania", launched.version.getId());
        assertEquals(Optional.of(json.toAbsolutePath().normalize()), launched.version.asFile());
        assertEquals(VersionFileLaunchTarget.NAME, launched.profile.target().name());
        assertEquals(List.of(), profileService.getProfiles());
    }

    @Test
    void somethingThatIsNotInstalledIsNotLaunched() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command("Aeromania")::call);

        assertTrue(e.getMessage().contains("Failed to find profile 'Aeromania'"), e.getMessage());
        // every launch target says why it does not launch it
        assertTrue(e.getMessage().contains(" - version:"), e.getMessage());
        assertTrue(e.getMessage().contains(" - version-file:"), e.getMessage());
        Mockito.verifyNoInteractions(clientLauncher);
    }

    @Test
    void profilesArePreferredOverEverythingElse() {
        // a profile for a version is named after the version
        Profile profile = profileService.save(new Profile("1.21.1", VersionArg.parse("fabric", "1.21.1"), root));

        Launched launched = assertThrows(Launched.class, command("1.21.1")::call);

        assertEquals("1.21.1", launched.profile.name());
        assertEquals(version(profile), version(launched.profile));
    }

    @Test
    void aVersionIsAVersionEvenIfAVersionJsonOfThatNameIsInstalled() {
        install("1.21.1");

        Launched launched = assertThrows(Launched.class, command("1.21.1")::call);

        // launched as the version 1.21.1, which finds that version.json itself, not as a file
        assertNull(launched.version);
        assertEquals(VersionHandle.VERSION, launched.profile.target().name());
        assertEquals(VersionArg.parse("vanilla", "1.21.1"), version(launched.profile));
    }

    @Test
    void versionArgumentsLaunchTheVersion() {
        Launched launched = assertThrows(Launched.class, command("fabric", "1.21.1")::call);

        assertNull(launched.version);
        assertEquals("fabric", version(launched.profile).platform());
        assertEquals(Optional.of(Side.CLIENT), launched.profile.side());
        assertEquals("fabric-1.21.1", launched.profile.name());
    }

    @Test
    void unknownNamesStillFail() {
        assertThrows(RuntimeException.class, command("Aeromania")::call);
        assertThrows(RuntimeException.class, command("forge", "1.21.1")::call);
        Mockito.verifyNoInteractions(clientLauncher);
        assertEquals(List.of(), clientTarget.accounts);
    }

    @Test
    void aStoredProfileForTheSameVersionIsUsed() {
        profileService.save(new Profile("my-fabric", VersionArg.parse("fabric", "1.21.1"), root.resolve("my-fabric")));

        Launched launched = assertThrows(Launched.class, command("fabric", "1.21.1")::call);

        assertEquals("my-fabric", launched.profile.name());
    }

    // --target

    @Test
    void aNamedTargetHandlesTheArguments() {
        // without --target this would launch the profile
        profileService.save(new Profile("1.21.1", VersionArg.parse("fabric", "1.21.1"), root));
        LaunchCommand command = command("1.21.1");
        command.setTarget("version");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("vanilla", "1.21.1"), version(launched.profile));
    }

    @Test
    void aNamedTargetThatCannotLaunchTheArgumentsSaysWhy() {
        LaunchCommand command = command("1.21.1");
        command.setTarget("version-file");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);

        assertTrue(e.getMessage().contains("version-file cannot launch '1.21.1'"), e.getMessage());
        assertTrue(e.getMessage().contains("Usage:"), e.getMessage());
    }

    @Test
    void anUnknownTargetListsTheAvailableOnes() {
        LaunchCommand command = command("1.21.1");
        command.setTarget("nope");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);

        assertTrue(e.getMessage().contains("packwiz, profile, server, version, version-file"), e.getMessage());
    }

    @Test
    void aProfileCanBeLaunchedThroughTheProfileTarget() {
        client("main");
        LaunchCommand command = command("main");
        command.setTarget("profile");

        Launched launched = assertThrows(Launched.class, command::call);

        // a profile launching "main", it launches what "main" launches
        assertEquals(VersionHandle.VERSION, launched.profile.target().name());
        assertEquals(VersionArg.parse("vanilla", "1.21.1"), version(launched.profile));
    }

    // what is saved

    @Test
    void whatAStoredProfileResolvesToIsSaved() {
        client("main");

        assertThrows(Launched.class, command("main")::call);

        VersionHandle handle = (VersionHandle) profileService.getProfile("main").orElseThrow().target().data();
        assertEquals(root.resolve("pinned-main"), handle.currentTarget());
    }

    @Test
    void optionsOfASingleLaunchAreNotSaved() {
        Profile stored = client("main");
        LaunchCommand command = command("main");
        command.setJvmArgs("-Xmx8G -Dfoo=bar");
        command.setGameArgs("--demo");
        command.setResolution("1024x768");
        command.setHeadless(true);
        command.setPatchers(new ArrayList<>(List.of("custom")));

        Launched launched = assertThrows(Launched.class, command::call);

        // launched with all of them
        assertEquals(List.of("-Xmx8G"), launched.profile.vmArgs());
        assertEquals("bar", launched.profile.systemProperties().get("foo"));
        assertTrue(launched.profile.patchers().contains("custom"));
        // but the stored profile only got what its launch target resolved
        Profile saved = profileService.getProfile("main").orElseThrow();
        assertEquals(stored.withTarget(saved.target()), saved);
        assertEquals(List.of(), saved.vmArgs());
        assertEquals(List.of(), saved.patchers());
        assertTrue(saved.options().isEmpty());
    }

    @Test
    void launchingArgumentsDoesNotCreateAProfile() {
        assertThrows(Launched.class, command("fabric", "1.21.1")::call);

        assertEquals(List.of(), profileService.getProfiles());
    }

    @Test
    void aSecondLaunchChangesNothing() {
        client("main");
        assertThrows(Launched.class, command("main")::call);
        Profile afterFirst = profileService.getProfile("main").orElseThrow();

        assertThrows(Launched.class, command("main")::call);

        assertSame(afterFirst, profileService.getProfile("main").orElseThrow());
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
        LaunchCommand command = command("main");
        command.setResolution("1024x768");
        command.setServer("example.com");

        Launched launched = assertThrows(Launched.class, command::call);

        LaunchOptions options = launched.profile.options();
        assertEquals(Optional.of(new LaunchOptions.Resolution(1024, 768)), options.resolution());
        assertEquals(
            Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, "example.com")), options.join()
        );
        // the quick play log is written to the .minecraft directory the game is launched from,
        // which the command does not know, it is decided when the process is built
        assertEquals(Optional.empty(), options.quickPlayPath());
    }

    @Test
    void theAccountIsGivenToTheLaunchTarget() {
        client("main");

        assertThrows(Launched.class, command("main")::call);

        assertEquals(List.of(account), clientTarget.accounts);
    }

    @Test
    void serverProfilesAreLaunchedByTheServerTarget() {
        server("srv");

        Launched launched = assertThrows(Launched.class, command("srv")::call);

        assertEquals("srv", launched.profile.name());
        // patchers are client-only
        assertEquals(List.of(), launched.profile.patchers());
        // a server needs no account
        assertEquals(List.of(), clientTarget.accounts);
        Mockito.verifyNoInteractions(lastUsedAccountService);
        // the java version the launch target found for the server is saved
        assertEquals(21, launched.profile.javaVersion());
        assertEquals(21, profileService.getProfile("srv").orElseThrow().javaVersion());
    }

    @Test
    void serversAreLaunchedByArguments() {
        server("srv");

        Launched launched = assertThrows(Launched.class, command("server", "1.21.1")::call);

        // the stored server for that version
        assertEquals("srv", launched.profile.name());
    }

    @Test
    void clientOnlyOptionsAreIgnoredForServers() {
        server("srv");
        LaunchCommand command = command("srv");
        command.setResolution("1024x768");

        assertThrows(Launched.class, command::call);
    }

    // profiles that are neither client nor server

    private Profile tool(String name) {
        FakeLaunchTarget tool = new FakeLaunchTarget("tool") {
            @Override
            public ProcessLauncher launcher(Profile profile, TypedMap options) {
                throw new Launched(profile, null);
            }
        };

        targets.with(tool);
        return profileService.save(new Profile(name, Optional.empty(), tool.handle("x"), root.resolve(name)));
    }

    @Test
    void aProfileWithoutSideIsLaunchedPlainly() {
        tool("tool");
        LaunchCommand command = command("tool");
        command.setJvmArgs("-Xmx1G");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals("tool", launched.profile.name());
        assertEquals(List.of("-Xmx1G"), launched.profile.vmArgs());
        // nothing that is specific to a Minecraft client or server
        assertEquals(List.of(), launched.profile.patchers());
        Mockito.verifyNoInteractions(lastUsedAccountService, authService, serverLauncher);
    }

    @Test
    void optionsForClientsAndServersAreRejectedForAProfileWithoutSide() {
        tool("tool");
        List<java.util.function.Consumer<LaunchCommand>> options = List.of(
            command -> command.setResolution("1024x768"),
            command -> command.setServer("example.com"),
            command -> command.setHeadless(true),
            command -> command.setOffline(true),
            command -> command.setAcceptEula(true)
        );

        for (java.util.function.Consumer<LaunchCommand> option : options) {
            LaunchCommand command = command("tool");
            option.accept(command);

            // not silently ignored, the profile is not treated as a client or a server
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
            assertTrue(e.getMessage().contains("tool"), e.getMessage());
        }
    }

    @Test
    void theEulaOptionIsRejectedForClients() {
        client("main");
        LaunchCommand command = command("main");
        command.setAcceptEula(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);

        assertTrue(e.getMessage().contains("not a server"), e.getMessage());
    }

    // dry runs

    @Test
    void dryRunDownloadsButDoesNotLaunch() {
        client("main");
        clientTarget.processLauncher = Mockito.mock(ProcessLauncher.class);
        LaunchCommand command = command("main");
        command.setDryRun(true);

        assertEquals(0, command.call());

        // resolved and the process built
        assertEquals(List.of(account), clientTarget.accounts);
        Mockito.verify(lifecycleService, Mockito.never()).wrap(Mockito.any(), Mockito.anyInt());
        Mockito.verify(lifecycleService, Mockito.never()).beforeLaunch(Mockito.any(), Mockito.anyBoolean());
        Mockito.verify(authService, Mockito.never()).refresh(Mockito.any());
        // a dry run installs, so what has been resolved is saved
        assertNotNull(((VersionHandle) profileService.getProfile("main").orElseThrow().target().data()).currentTarget());
    }

    @Test
    void dryRunDoesNotNeedAnAccount() {
        client("main");
        Mockito.when(lastUsedAccountService.getLastUsedAccount()).thenReturn(Optional.empty());
        clientTarget.processLauncher = Mockito.mock(ProcessLauncher.class);
        LaunchCommand command = command("main");
        command.setDryRun(true);

        assertEquals(0, command.call());

        assertEquals(List.of(Account.defaultOfflineAccount()), clientTarget.accounts);
    }

    @Test
    void dryRunOfAVersionJsonDownloadsButDoesNotLaunch() {
        Path json = install("Aeromania");
        Mockito.doReturn(Mockito.mock(ProcessLauncher.class))
            .when(clientLauncher).launcher(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        LaunchCommand command = command(json.toString());
        command.setDryRun(true);

        assertEquals(0, command.call());

        Mockito.verify(clientLauncher).launcher(
            Mockito.any(), Mockito.eq(account), Mockito.argThat(version -> version.getId().equals("Aeromania")), Mockito.any()
        );
        Mockito.verify(lifecycleService, Mockito.never()).wrap(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void dryRunExitsWithAnErrorIfADownloadFails() {
        client("main");
        clientTarget.failure = new VerificationException("client jar has the wrong sha1");
        LaunchCommand command = command();

        int exitCode = commandLine(command).execute("--dry-run", "main");

        assertNotEquals(0, exitCode);
        Mockito.verify(lifecycleService, Mockito.never()).wrap(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void dryRunExitsWithZeroOnTheCommandLine() {
        client("main");
        clientTarget.processLauncher = Mockito.mock(ProcessLauncher.class);

        assertEquals(0, commandLine(command()).execute("--dry-run", "main"));
    }

    @Test
    void theTargetCanBeGivenOnTheCommandLine() {
        clientTarget.processLauncher = Mockito.mock(ProcessLauncher.class);
        serverTarget.processLauncher = Mockito.mock(ProcessLauncher.class);

        assertEquals(0, commandLine(command()).execute("--dry-run", "--target", "version", "fabric", "1.21.1"));
        assertEquals(0, commandLine(command()).execute("--dry-run", "-t", "server", "server", "1.21.1"));
        // a client version is not launched by the target for servers
        assertNotEquals(0, commandLine(command()).execute("--dry-run", "-t", "server", "fabric", "1.21.1"));
    }

    private CommandLine commandLine(LaunchCommand command) {
        return new CommandLine(command, new CommandLine.IFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <K> K create(Class<K> cls) throws Exception {
                if (cls == LaunchTargetCompletions.class) {
                    return (K) Mockito.mock(LaunchTargetCompletions.class);
                }

                if (cls == LaunchTargetNameCompletions.class) {
                    return (K) Mockito.mock(LaunchTargetNameCompletions.class);
                }

                return CommandLine.defaultFactory().create(cls);
            }
        });
    }

    // packwiz

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
        // a pack can be both
        assertTrue(e.getMessage().contains("client or server"), e.getMessage());
        assertTrue(e.getMessage().contains("[client|server]"), e.getMessage());
    }

    @Test
    void packwizLaunchesTheClient() throws IOException {
        Path pack = singleLoaderPack();
        LaunchCommand command = packwizCommand(pack, "client");

        Launched launched = assertThrows(Launched.class, command::call);

        // the version the pack asks for, launched by the launch target for versions in the directory of the pack
        assertEquals(VersionHandle.VERSION, launched.profile.target().name());
        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14"), version(launched.profile));
        assertEquals(Optional.of(Side.CLIENT), launched.profile.side());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
        assertTrue(launched.profile.patchers().contains("log4j"));
    }

    @Test
    void packwizSideIsCaseInsensitive() throws IOException {
        LaunchCommand command = packwizCommand(singleLoaderPack(), "CLIENT");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(Optional.of(Side.CLIENT), launched.profile.side());
    }

    @Test
    void packwizLaunchesTheServer() throws IOException {
        Path pack = singleLoaderPack();
        LaunchCommand command = packwizCommand(pack, "server");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionHandle.SERVER, launched.profile.target().name());
        assertEquals(VersionArg.parse("server", "fabric", "1.21.1", "0.16.14"), version(launched.profile));
        assertEquals(Optional.of(Side.SERVER), launched.profile.side());
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
        // patchers are client-only
        assertEquals(List.of(), launched.profile.patchers());
        assertEquals(List.of("nogui"), launched.profile.gameArgs());
    }

    @Test
    void packwizVanillaPackLaunchesVanilla() throws IOException {
        LaunchCommand command = packwizCommand(packToml(""), "client");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("vanilla", "1.21.1"), version(launched.profile));
    }

    @Test
    void packwizWithMultipleLoadersNeedsTheLoader() throws IOException {
        for (String[] args : List.of(new String[]{"client"}, new String[]{})) {
            LaunchCommand command = packwizCommand(multiLoaderPack(), args);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
            if (args.length > 0) {
                assertTrue(e.getMessage().contains("multiple loaders"), e.getMessage());
                assertTrue(e.getMessage().contains("quilt, fabric"), e.getMessage());
            }
        }
    }

    @Test
    void packwizWithSideAndLoaderLaunchesThatLoader() throws IOException {
        Path pack = multiLoaderPack();
        LaunchCommand command = packwizCommand(pack, "server", "Fabric");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("server", "fabric", "1.21.1", "0.16.14"), version(launched.profile));
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizWithSideAndUnknownLoaderThrows() throws IOException {
        LaunchCommand command = packwizCommand(multiLoaderPack(), "client", "forge");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("no loader forge"), e.getMessage());
        assertTrue(e.getMessage().contains("quilt, fabric"), e.getMessage());
    }

    @Test
    void packwizWithSomethingThatIsNotASideThrows() throws IOException {
        // not a side, so it is taken for a loader, which the pack does not have
        LaunchCommand command = packwizCommand(singleLoaderPack(), "neither");

        assertThrows(IllegalArgumentException.class, command::call);
    }

    @Test
    void packwizAcceptsTheDirectoryOfThePack() throws IOException {
        Path pack = singleLoaderPack();
        LaunchCommand command = packwizCommand(pack.getParent(), "client");

        Launched launched = assertThrows(Launched.class, command::call);

        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14"), version(launched.profile));
        assertEquals(pack.toAbsolutePath().getParent(), launched.profile.path());
    }

    @Test
    void packwizWithDirectoryWithoutPackTomlThrows() throws IOException {
        LaunchCommand command = packwizCommand(Files.createDirectories(root.resolve("empty")), "client");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("contains no pack.toml"), e.getMessage());
    }

    @Test
    void packwizWithMissingFileThrows() {
        LaunchCommand command = packwizCommand(root.resolve("missing").resolve("pack.toml"), "client");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::call);
        assertTrue(e.getMessage().contains("does not exist"), e.getMessage());
    }

    @Test
    void packwizOptionIsTheSameAsTheTarget() throws IOException {
        Path pack = singleLoaderPack();
        LaunchCommand viaTarget = command(pack.toString(), "client");
        viaTarget.setTarget(PackwizLaunchTarget.NAME);

        Launched option = assertThrows(Launched.class, packwizCommand(pack, "client")::call);
        Launched target = assertThrows(Launched.class, viaTarget::call);

        assertEquals(option.profile, target.profile);
    }

    @Test
    void aPackIsDetectedWithoutNamingTheTarget() throws IOException {
        Path pack = singleLoaderPack();

        // a directory with a pack.toml can only be a pack
        Launched launched = assertThrows(Launched.class, command(pack.getParent().toString(), "client")::call);

        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14"), version(launched.profile));
    }

    @Test
    void packwizOptionCannotBeCombinedWithAnotherTarget() throws IOException {
        LaunchCommand command = packwizCommand(singleLoaderPack(), "client");
        command.setTarget("version");

        assertThrows(IllegalArgumentException.class, command::call);
    }

    @Test
    void aStoredProfileForAPackIsUsedAndRemembersWhatThePackResolvedTo() throws IOException {
        Path pack = singleLoaderPack();
        Profile stored = profileService.save(launchTargetService.create(
            "my-pack",
            launchTargetService.parse(PackwizLaunchTarget.NAME, TargetArgs.of(pack.toString(), "client")),
            new TypedMapImpl()
        ));
        assertEquals(PackwizLaunchTarget.NAME, stored.target().name());
        assertEquals("packwiz:my pack", launchTargetService.getLabel(stored));

        Launched launched = assertThrows(Launched.class, packwizCommand(pack, "client")::call);

        assertEquals("my-pack", launched.profile.name());
        // the stored profile still launches the pack, and what the version of the pack resolved to is pinned in it
        Profile saved = profileService.getProfile("my-pack").orElseThrow();
        assertEquals(PackwizLaunchTarget.NAME, saved.target().name());
        assertEquals(Optional.of(root.resolve("pinned-my-pack")), launchTargetService.describe(saved).file());
        // launching it by its name is the same
        assertEquals("my-pack", assertThrows(Launched.class, command("my-pack")::call).profile.name());
    }

    @Test
    void aPackThatChangedIsResolvedAgain() throws IOException {
        Path pack = singleLoaderPack();
        profileService.save(launchTargetService.create(
            "my-pack",
            launchTargetService.parse(PackwizLaunchTarget.NAME, TargetArgs.of(pack.toString(), "client")),
            new TypedMapImpl()
        ));
        assertThrows(Launched.class, command("my-pack")::call);

        // the pack is updated to another loader
        Files.writeString(pack, Files.readString(pack).replace("fabric = \"0.16.14\"", "quilt = \"0.26.0\""));
        // the loader that has been chosen for the profile is gone
        assertThrows(RuntimeException.class, command("my-pack")::call);

        // a pack with a single loader does not need one to be chosen
        Files.writeString(pack, Files.readString(pack).replace("quilt = \"0.26.0\"", "fabric = \"0.16.14\""));
        Launched launched = assertThrows(Launched.class, command("my-pack")::call);
        assertEquals(VersionArg.parse("fabric", "1.21.1", "0.16.14"), version(launched.profile));
    }

}
