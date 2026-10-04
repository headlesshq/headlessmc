package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.server.FakeServerService;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
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

    @BeforeEach
    void setup() {
        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        vanilla.withServerSupport(new ServerSupport(
            dir -> dir.resolve("server.jar"),
            (id, dir, args) -> {
                installations.add(new Installation(id, dir));
                return new ServerInstaller.Installation(21);
            }
        ));

        platformService = new FakePlatformService(vanilla);
        profileService = new FakeProfileService(root.resolve("profiles"));
        serverService = new FakeServerService(profileService);
        appFiles = TestFiles.appFiles(root);
        mcFiles = TestFiles.mcFiles(root);
    }

    private AddCommand add() {
        return new AddCommand(
            platformService, profileService, serverService,
            new DefaultFileService(new DefaultFileSystemProvider()), appFiles, mcFiles, console
        );
    }

    @Test
    void addWithoutVersionThrows() {
        assertThrows(IllegalArgumentException.class, add()::run);
    }

    @Test
    void addInstallsServerWithDefaultName() {
        AddCommand command = add();
        command.setVersionArg(List.of("1.21.1"));
        command.run();

        assertEquals(1, installations.size());
        Installation installation = installations.getFirst();
        assertEquals("1.21.1", installation.id().getVersion().getName());

        List<Profile> servers = serverService.listServers();
        assertEquals(1, servers.size());
        Profile server = servers.getFirst();
        assertEquals(appFiles.getServerDir().resolve(server.name()), installation.dir());
        assertEquals(21, server.javaVersion());
        assertEquals(Optional.of(Side.SERVER), server.version().side());
        assertTrue(console.output().contains("Installed server"));
    }

    @Test
    void addUsesCustomNameAndDir() {
        AddCommand command = add();
        command.setVersionArg(List.of("1.21.1"));
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
        ), "my-server", 21);

        AddCommand command = add();
        command.setVersionArg(List.of("1.21.1"));
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
        command.setVersionArg(List.of("1.21.1"));
        command.setName("my-server");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("client profile"));
    }

    @Test
    void addOnPlatformWithoutServerSupportThrows() {
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));

        AddCommand command = add();
        command.setVersionArg(List.of("1.21.1"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("does not support servers"));
    }

    @Test
    void listShowsOnlyServers() {
        profileService.createDefault("client", VersionArg.parse("vanilla", "1.21.1"));
        serverService.add(root.resolve("srv"), VersionID.resolve(
            platformService, VersionArg.parse("server", "vanilla", "1.21.1")
        ), "srv", 21);

        new ListCommand(serverService, tables, console).run();

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
        ), "srv", 21);

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
        ), "srv", 21);

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

}
