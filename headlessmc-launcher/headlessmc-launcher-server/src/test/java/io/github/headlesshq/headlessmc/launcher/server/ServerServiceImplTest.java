package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstallation;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServerServiceImplTest {
    @TempDir
    Path root;

    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private ServerServiceImpl service;

    @BeforeEach
    void setup() {
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"));
        profileService = new FakeProfileService(root);
        service = new ServerServiceImpl(profileService);
    }

    private VersionID id() {
        return VersionID.resolve(platformService, VersionArg.parse("vanilla", "1.21.1"));
    }

    @Test
    void addsServerProfilesWithADefaultName() {
        Profile server = service.add(root.resolve("srv"), id(), null, new ServerInstallation(21));

        assertEquals("server-1.21.1", server.name());
        assertEquals(Optional.of(Side.SERVER), server.side());
        assertEquals(VersionHandle.SERVER, server.target().name());
        // just installed, this is the version it currently is
        VersionHandle handle = (VersionHandle) server.target().data();
        assertEquals(VersionArg.parse("server", "vanilla", "1.21.1"), handle.version());
        assertEquals(handle.version(), handle.current());
        assertEquals(21, server.javaVersion());
        assertEquals(root.resolve("srv"), server.path());
        assertEquals(List.of(), server.vmArgs());
    }

    @Test
    void theBuildOfTheInstallationIsTheCurrentVersion() {
        FakePlatformService platforms = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1"), FakePlatform.create("paper", new String[]{"1.21.1", "130", "129"})
        );
        VersionID paper = VersionID.resolve(platforms, VersionArg.parse("paper", "1.21.1"));

        Profile server = service.add(root.resolve("srv"), paper, null, new ServerInstallation(21, List.of(), "130"));

        VersionHandle handle = (VersionHandle) server.target().data();
        // any build has been asked for, build 130 has been installed
        assertEquals(VersionArg.parse("server", "paper", "1.21.1"), handle.version());
        assertEquals(VersionArg.parse("server", "paper", "1.21.1", "130"), handle.current());
        assertEquals("server-paper-1.21.1", server.name());
    }

    @Test
    void aBuildThePlatformDoesNotKnowIsNotStored() {
        FakePlatformService platforms = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1"), FakePlatform.create("neoforge", new String[]{"1.21.1", "266", "265"})
        );
        VersionID neoforge = VersionID.resolve(platforms, VersionArg.parse("neoforge", "1.21.1"));

        // an installer reporting a name its platform has no build for: the version could never be resolved again
        Profile server = service.add(root.resolve("srv"), neoforge, null, new ServerInstallation(21, List.of(), "21.1.266"));

        VersionHandle handle = (VersionHandle) server.target().data();
        assertEquals(VersionArg.parse("server", "neoforge", "1.21.1"), handle.current());
        // a vanilla version has no builds at all
        Profile vanilla = service.add(root.resolve("v"), id(), "v", new ServerInstallation(21, List.of(), "130"));
        assertEquals(VersionArg.parse("server", "vanilla", "1.21.1"), ((VersionHandle) vanilla.target().data()).current());
    }

    @Test
    void addsServerProfilesWithTheVmArgsOfTheInstallation() {
        ServerInstallation installation = new ServerInstallation(21, List.of("-XX:+UseG1GC", "-XX:+AlwaysPreTouch"));

        Profile server = service.add(root.resolve("srv"), id(), "my-server", installation);

        assertEquals(List.of("-XX:+UseG1GC", "-XX:+AlwaysPreTouch"), server.vmArgs());
        assertEquals(List.of("nogui"), server.gameArgs());
    }

    @Test
    void addsServerProfilesWithAGivenName() {
        assertEquals("my-server", service.add(root.resolve("srv"), id(), "my-server", new ServerInstallation(21)).name());
    }

    @Test
    void findsServersIgnoringCase() {
        service.add(root.resolve("srv"), id(), "my-server", new ServerInstallation(21));

        assertTrue(service.getServer("MY-SERVER").isPresent());
        assertTrue(service.getServer("nope").isEmpty());
    }

    @Test
    void listsOnlyServerProfiles() {
        service.add(root.resolve("srv"), id(), "my-server", new ServerInstallation(21));
        profileService.createDefault("client", VersionArg.parse("vanilla", "1.21.1"));

        assertEquals(List.of("my-server"), service.listServers().stream().map(Profile::name).toList());
    }

    @Test
    void savesAndRemovesServers() {
        Profile server = service.add(root.resolve("srv"), id(), "my-server", new ServerInstallation(21));

        service.save(server.withJavaVersion(17));
        assertEquals(17, service.getServer("my-server").orElseThrow().javaVersion());

        service.remove(server);
        assertEquals(List.of(), service.listServers());
    }

    @Test
    void eulaStatusIsOnlySavedWhenItChanges() {
        Profile server = service.add(root.resolve("srv"), id(), "my-server", new ServerInstallation(21));
        assertEquals(EulaStatus.UNKNOWN, server.eulaStatus());

        assertSame(server, service.setEulaStatus(server, EulaStatus.UNKNOWN));

        Profile updated = service.setEulaStatus(server, EulaStatus.EXISTS);
        assertEquals(EulaStatus.EXISTS, updated.eulaStatus());
        assertEquals(EulaStatus.EXISTS, service.getServer("my-server").orElseThrow().eulaStatus());
    }

}
