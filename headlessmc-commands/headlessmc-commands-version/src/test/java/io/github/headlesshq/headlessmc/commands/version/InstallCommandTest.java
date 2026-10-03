package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
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
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InstallCommandTest {
    private record ClientInstallation(VersionID id, Path dir, TypedMap args) {}

    private record ServerInstallation(VersionID id, Path dir) {}

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final List<ClientInstallation> clientInstalls = new ArrayList<>();
    private final List<ServerInstallation> serverInstalls = new ArrayList<>();

    private FakeVanillaPlatform vanilla;
    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private FakeServerService serverService;
    private McFiles mcFiles;
    private AppFiles appFiles;

    @BeforeEach
    void setup() {
        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        ClientInstaller clientInstaller = (id, mcDir, args) -> {
            clientInstalls.add(new ClientInstallation(id, mcDir, args));
            return new FakeVersion(id.getVersion().getName());
        };
        ServerInstaller serverInstaller = (id, dir, args) -> {
            serverInstalls.add(new ServerInstallation(id, dir));
            return new ServerInstaller.Installation(21);
        };
        vanilla.withClientSupport(new ClientSupport(new UnsupportedMatcher(), clientInstaller));
        vanilla.withServerSupport(new ServerSupport(serverDir -> serverDir.resolve("server.jar"), serverInstaller));

        platformService = new FakePlatformService(vanilla);
        profileService = new FakeProfileService(root.resolve("profiles"));
        serverService = new FakeServerService(profileService);
        mcFiles = TestFiles.mcFiles(root);
        appFiles = TestFiles.appFiles(root);
    }

    private InstallCommand command() {
        return new InstallCommand(
            platformService, profileService, serverService,
            new DefaultFileService(new DefaultFileSystemProvider()), appFiles, mcFiles, console
        );
    }

    @Test
    void emptyVersionArgThrows() {
        assertThrows(IllegalArgumentException.class, command()::run);
    }

    @Test
    void installsClientIntoMcDir() {
        InstallCommand command = command();
        command.setVersionArg(List.of("1.21.1"));
        command.run();

        assertEquals(1, clientInstalls.size());
        ClientInstallation install = clientInstalls.getFirst();
        assertEquals("1.21.1", install.id().getVersion().getName());
        assertEquals(mcFiles.getMcDir(), install.dir());
        assertEquals(false, install.args().get(ClientInstaller.FORCE_INSTALL));
        assertTrue(console.output().contains("Installed version"));
    }

    @Test
    void forceCustomUrlAndDirArePassedToClientInstaller() {
        InstallCommand command = command();
        command.setVersionArg(List.of("1.21.1"));
        command.setForce(true);
        command.setCustomURL("https://example.com/installer.jar");
        command.setCustomDir(root.resolve("custom").toString());
        command.run();

        ClientInstallation install = clientInstalls.getFirst();
        assertEquals(true, install.args().get(ClientInstaller.FORCE_INSTALL));
        assertEquals(URI.create("https://example.com/installer.jar"),
            install.args().get(ClientInstaller.CUSTOM_INSTALLER_URL));
        assertEquals(root.resolve("custom"), install.dir());
    }

    @Test
    void unknownVersionThrows() {
        InstallCommand command = command();
        command.setVersionArg(List.of("1.8.9"));
        assertThrows(RuntimeException.class, command::run);
    }

    @Test
    void platformWithoutClientSupportThrows() {
        FakeVanillaPlatform noSupport = new FakeVanillaPlatform("1.21.1");
        platformService = new FakePlatformService(noSupport);

        InstallCommand command = command();
        command.setVersionArg(List.of("1.21.1"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("does not support clients"));
    }

    @Test
    void installsServerAndAddsProfile() {
        InstallCommand command = command();
        command.setVersionArg(List.of("server", "1.21.1"));
        command.run();

        assertEquals(1, serverInstalls.size());
        ServerInstallation install = serverInstalls.getFirst();
        assertEquals("1.21.1", install.id().getVersion().getName());

        List<Profile> servers = serverService.listServers();
        assertEquals(1, servers.size());
        Profile server = servers.getFirst();
        assertEquals(21, server.javaVersion());
        assertEquals(Optional.of(Side.SERVER), server.version().side());
        assertEquals(install.dir(), server.path());
        assertTrue(console.output().contains("Installed server"));
    }

    @Test
    void serverInstallUsesCustomName() {
        InstallCommand command = command();
        command.setVersionArg(List.of("server", "1.21.1"));
        command.setName("my-server");
        command.run();

        assertTrue(serverService.getServer("my-server").isPresent());
        assertEquals(appFiles.getServerDir().resolve("my-server"), serverInstalls.getFirst().dir());
    }

    @Test
    void existingServerRequiresForce() {
        serverService.add(root.resolve("existing"), VersionID.resolve(
            platformService, VersionArg.parse("server", "vanilla", "1.21.1")
        ), "my-server", 21);

        InstallCommand command = command();
        command.setVersionArg(List.of("server", "1.21.1"));
        command.setName("my-server");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("already exists"));

        command.setForce(true);
        command.run();
        assertEquals(1, serverInstalls.size());
    }

    @Test
    void clientProfileWithSameNameBlocksServerInstall() {
        profileService.createDefault("my-server", VersionArg.parse("vanilla", "1.21.1"));

        InstallCommand command = command();
        command.setVersionArg(List.of("server", "1.21.1"));
        command.setName("my-server");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("client profile"));
    }

    private static final class UnsupportedMatcher implements io.github.headlesshq.headlessmc.platform.client.VersionMatcher {
        @Override
        public VersionID match(io.github.headlesshq.headlessmc.platform.PlatformService platformService,
                               Version version,
                               io.github.headlesshq.headlessmc.version.VersionProcessor processor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canMatch(io.github.headlesshq.headlessmc.platform.PlatformService platformService,
                                Version version,
                                io.github.headlesshq.headlessmc.version.VersionProcessor processor) {
            return false;
        }

        @Override
        public String getPlatformName() {
            return "vanilla";
        }
    }

}
