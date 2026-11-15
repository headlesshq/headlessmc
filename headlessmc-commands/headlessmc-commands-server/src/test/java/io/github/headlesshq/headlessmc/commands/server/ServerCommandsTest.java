package io.github.headlesshq.headlessmc.commands.server;

import java.util.jar.Manifest;
import java.util.jar.JarOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.Attributes;
import java.nio.ByteBuffer;
import io.github.headlesshq.headlessmc.launcher.server.ScriptLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.server.JarLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.TestEvent;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileMover;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.FakeVersionLaunchTarget;
import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.launcher.server.ServerLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import org.mockito.Mockito;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.server.FakeServerService;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstallation;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServerCommandsTest {
    private record Installation(VersionID id, Path dir) {}

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();

    private final List<Installation> installations = new ArrayList<>();

    private FakeVanillaPlatform vanilla;
    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private FakeServerService serverService;
    private AppFiles appFiles;
    private McFiles mcFiles;
    private LaunchTargetService launchTargetService;

    @BeforeEach
    void setup() {
        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        vanilla.withServerSupport(new ServerSupport(
            dir -> dir.resolve("server.jar"),
            (id, dir, args) -> {
                installations.add(new Installation(id, dir));
                return new ServerInstallation(21);
            }
        ));

        platformService = new FakePlatformService(vanilla);
        profileService = new FakeProfileService(root.resolve("profiles"));
        serverService = new FakeServerService(profileService);
        appFiles = TestFiles.appFiles(root);
        mcFiles = TestFiles.mcFiles(root);
    }

    private AddCommand add() {
        // the real launch target for servers, it installs the server of a new profile
        TestLaunchTargets targets = new TestLaunchTargets(profileService);
        targets.with(new ServerLaunchTarget(
            platformService, Mockito.mock(ServerLauncher.class), targets.jsonService(), appFiles
        ));
        launchTargetService = targets.service();
        return new AddCommand(
            launchTargetService, profileService, McFilesService.of(mcFiles),
            new DefaultFileService(new DefaultFileSystemProvider()), console
        );
    }

    @Test
    void addWithoutVersionThrows() {
        assertThrows(IllegalArgumentException.class, add()::run);
    }

    @Test
    void addInstallsServerWithDefaultName() {
        AddCommand command = add();
        command.setTargetArgs(List.of("1.21.1"));
        command.run();

        assertEquals(1, installations.size());
        Installation installation = installations.getFirst();
        assertEquals("1.21.1", installation.id().getVersion().getName());

        List<Profile> servers = serverService.listServers();
        assertEquals(1, servers.size());
        Profile server = servers.getFirst();
        assertEquals(appFiles.getServerDir().resolve(server.name()), installation.dir());
        assertEquals(21, server.javaVersion());
        assertEquals(Optional.of(Side.SERVER), server.side());
        assertEquals(VersionHandle.SERVER, server.target().name());
        assertEquals(List.of("nogui"), server.gameArgs());
        assertTrue(console.output().contains("Installed server"));
    }

    @Test
    void addUsesCustomNameAndDir() {
        AddCommand command = add();
        command.setTargetArgs(List.of("1.21.1"));
        command.setName("my-server");
        command.setCustomDir(root.resolve("custom").toString());
        command.run();

        assertEquals(root.resolve("custom"), installations.getFirst().dir());
        assertTrue(serverService.getServer("my-server").isPresent());
    }

    @Test
    void addExistingServerRequiresForce() {
        serverService.add(root.resolve("existing"), VersionID.resolve(
            platformService, VersionArg.parse("server", "vanilla", "1.21.1")
        ), "my-server", new ServerInstallation(21));

        AddCommand command = add();
        command.setTargetArgs(List.of("1.21.1"));
        command.setName("my-server");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("already exists"));

        command.setForce(true);
        command.run();
        assertEquals(1, installations.size());
    }

    @Test
    void addFailsIfClientProfileWithSameNameExists() {
        profileService.createDefault("my-server", VersionArg.parse("vanilla", "1.21.1"));

        AddCommand command = add();
        command.setTargetArgs(List.of("1.21.1"));
        command.setName("my-server");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("client profile"));
    }

    @Test
    void addOnPlatformWithoutServerSupportThrows() {
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));

        AddCommand command = add();
        command.setTargetArgs(List.of("1.21.1"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("does not support servers"));
    }

    @Test
    void listShowsOnlyServers() {
        profileService.createDefault("client", VersionArg.parse("vanilla", "1.21.1"));
        serverService.add(root.resolve("srv"), VersionID.resolve(
            platformService, VersionArg.parse("server", "vanilla", "1.21.1")
        ), "srv", new ServerInstallation(21));

        new ListCommand(TestLaunchTargets.versions(platformService, profileService, root), serverService, tables, console).run();

        String output = console.output();
        assertTrue(output.contains("srv"));
        assertFalse(output.contains("client"));
    }

    @Test
    void removeWithoutNameThrows() {
        RemoveCommand command = new RemoveCommand(serverService, new DefaultFileService(new DefaultFileSystemProvider()), console);
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void removeUnknownServerThrows() {
        RemoveCommand command = new RemoveCommand(serverService, new DefaultFileService(new DefaultFileSystemProvider()), console);
        command.setName("nope");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void removeDeletesServerDirectory() {
        Path dir = createDir(root.resolve("srv"));
        serverService.add(dir, VersionID.resolve(
            platformService, VersionArg.parse("server", "vanilla", "1.21.1")
        ), "srv", new ServerInstallation(21));

        RemoveCommand command = new RemoveCommand(serverService, new DefaultFileService(new DefaultFileSystemProvider()), console);
        command.setName("srv");
        command.run();

        assertTrue(serverService.getServer("srv").isEmpty());
        assertFalse(Files.exists(dir));
        assertTrue(console.output().contains("Removed server srv"));
    }

    @Test
    void serverCompletionsListServerNames() {
        serverService.add(root.resolve("srv"), VersionID.resolve(
            platformService, VersionArg.parse("server", "vanilla", "1.21.1")
        ), "srv", new ServerInstallation(21));

        List<String> names = new ArrayList<>();
        new ServerCompletions(serverService).forEach(names::add);
        assertEquals(List.of("srv"), names);
    }

    private Path createDir(Path path) {
        try {
            return Files.createDirectories(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // server add for something that is not a version

    private AddCommand addExisting(String... targetArgs) {
        TestLaunchTargets targets = new TestLaunchTargets(profileService);
        ServerLauncher serverLauncher = Mockito.mock(ServerLauncher.class);
        DefaultFileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
        targets.with(
            new ServerLaunchTarget(platformService, serverLauncher, targets.jsonService(), appFiles),
            new JarLaunchTarget(serverLauncher, fileService, targets.jsonService()),
            new ScriptLaunchTarget(serverLauncher, fileService, targets.jsonService())
        );
        AddCommand command = new AddCommand(targets.service(), profileService, McFilesService.of(mcFiles), fileService, console);
        command.setTargetArgs(List.of(targetArgs));
        return command;
    }

    /** A jar that can be launched, its main class has been compiled for Java 17. */
    private Path jar(String name) throws IOException {
        Path file = Files.createDirectories(root.resolve("existing")).resolve(name);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "test.Main");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file), manifest)) {
            jar.putNextEntry(new JarEntry("test/Main.class"));
            jar.write(ByteBuffer.allocate(8).putInt(0xCAFEBABE).putShort((short) 0).putShort((short) 61).array());
            jar.closeEntry();
        }

        return file;
    }

    @Test
    void addsAnExistingServerJar() throws IOException {
        Path jar = jar("paper.jar");

        AddCommand command = addExisting(jar.toString());
        command.setName("lobby");
        command.run();

        Profile server = serverService.getServer("lobby").orElseThrow();
        // "server add" adds servers, also if a jar does not say what it is
        assertEquals(Optional.of(Side.SERVER), server.side());
        assertEquals(JarLaunchTarget.NAME, server.target().name());
        // it runs where it is, nothing is installed or copied
        assertEquals(jar.getParent(), server.path());
        assertEquals(List.of(), installations);
        assertEquals(17, server.javaVersion());
        assertTrue(console.output().contains("Installed server lobby for jar:paper.jar"), console.output());
    }

    @Test
    void addsAnExistingStartScript() throws IOException {
        Path script = Files.writeString(Files.createDirectories(root.resolve("existing")).resolve("run.sh"), "#!/bin/sh");

        AddCommand command = addExisting(script.toString());
        command.setName("modded");
        command.run();

        Profile server = serverService.getServer("modded").orElseThrow();
        assertEquals(ScriptLaunchTarget.NAME, server.target().name());
        assertEquals(Optional.of(Side.SERVER), server.side());
        assertEquals(script.getParent(), server.path());
    }

    @Test
    void anExistingServerNeedsAName() throws IOException {
        Path jar = jar("paper.jar");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, addExisting(jar.toString())::run);

        assertEquals("Please specify a name for the server with --name.", e.getMessage());
        assertEquals(List.of(), serverService.listServers());
    }

    @Test
    void serverAddDoesNotAddClients() throws IOException {
        Path jar = jar("client.jar");

        AddCommand command = addExisting("client", jar.toString());
        command.setName("client");

        assertThrows(IllegalArgumentException.class, command::run);
        assertEquals(List.of(), profileService.getProfiles());
    }

    // server move

    private MoveCommand move(String name) {
        TestLaunchTargets targets = new TestLaunchTargets(profileService);
        ProfileLaunchTarget profileTarget = targets.profileTarget();
        targets.with(
            new ServerLaunchTarget(platformService, Mockito.mock(ServerLauncher.class), targets.jsonService(), appFiles),
            new FakeVersionLaunchTarget(platformService, targets.jsonService(), Side.CLIENT, root),
            profileTarget
        );
        DefaultFileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
        ProfileMover mover = new ProfileMover(
            targets.service(), profileTarget, profileService, McFilesService.of(mcFiles), fileService,
            new TestEvent<>(), new TestEvent<>()
        );

        MoveCommand command = new MoveCommand(targets.service(), profileService, serverService, mover, fileService, console);
        command.setName(name);
        return command;
    }

    /** Adds a server {@code lobby} whose server.jar exists. */
    private Profile addLobby() throws IOException {
        AddCommand command = add();
        command.setTargetArgs(List.of("1.21.1"));
        command.setName("lobby");
        command.run();
        Profile lobby = serverService.getServer("lobby").orElseThrow();
        Files.createDirectories(lobby.path());
        Files.writeString(lobby.path().resolve("server.jar"), "jar");
        return lobby;
    }

    @Test
    void moveMovesTheDirectoryOfAServer() throws IOException {
        Profile lobby = addLobby();
        Path destination = root.resolve("srv").resolve("lobby");

        MoveCommand command = move("lobby");
        command.setDirectory(destination.toString());
        command.run();

        Profile moved = serverService.getServer("lobby").orElseThrow();
        assertEquals(destination, moved.path());
        assertEquals("jar", Files.readString(destination.resolve("server.jar")));
        assertFalse(Files.exists(lobby.path()));
        // the server is pinned to its executable, the pin moved with it
        VersionHandle handle = (VersionHandle) moved.target().data();
        assertEquals(destination.resolve("server.jar"), handle.currentTarget());
        assertTrue(console.output().contains("Moved server lobby, moved " + lobby.path() + " to " + destination), console.output());
    }

    @Test
    void moveRenamesAServer() throws IOException {
        Profile lobby = addLobby();

        MoveCommand command = move("lobby");
        command.setNewName("hub");
        command.run();

        assertTrue(serverService.getServer("lobby").isEmpty());
        Profile hub = serverService.getServer("hub").orElseThrow();
        // only the name changed, the server stays where it is
        assertEquals(lobby.path(), hub.path());
        assertTrue(Files.exists(lobby.path().resolve("server.jar")));
        assertTrue(console.output().contains("Renamed server lobby to hub"), console.output());
    }

    @Test
    void moveOnlyMovesServers() throws IOException {
        addLobby();
        profileService.save(new Profile("main", VersionArg.parse("1.21.1"), root.resolve("main")));

        MoveCommand client = move("main");
        client.setNewName("renamed");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, client::run);
        assertEquals("Failed to find server with name main", e.getMessage());
        assertTrue(profileService.getProfile("main").isPresent());

        assertThrows(IllegalArgumentException.class, move(null)::run);
        e = assertThrows(IllegalArgumentException.class, move("lobby")::run);
        assertTrue(e.getMessage().contains("Please specify where to move server lobby: a directory and/or --name <name>."), e.getMessage());
    }

    @Test
    void aServerIsNotRenamedOverAClientProfile() throws IOException {
        addLobby();
        profileService.save(new Profile("main", VersionArg.parse("1.21.1"), root.resolve("main")));

        MoveCommand command = move("lobby");
        command.setNewName("main");

        assertThrows(IllegalArgumentException.class, command::run);
        // not even with --force
        command.setForce(true);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertEquals("A client profile with name main already exists.", e.getMessage());
        assertTrue(serverService.getServer("lobby").isPresent());
        assertTrue(profileService.getProfile("main").orElseThrow().isClient());
    }

}
