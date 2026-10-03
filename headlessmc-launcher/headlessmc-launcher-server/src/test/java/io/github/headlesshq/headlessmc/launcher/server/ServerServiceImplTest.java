package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
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
        Profile server = service.add(root.resolve("srv"), id(), null, 21);

        assertEquals("server-1.21.1", server.name());
        assertEquals(Optional.of(Side.SERVER), server.version().side());
        assertEquals(21, server.javaVersion());
        assertEquals(root.resolve("srv"), server.path());
    }

    @Test
    void addsServerProfilesWithAGivenName() {
        assertEquals("my-server", service.add(root.resolve("srv"), id(), "my-server", 21).name());
    }

    @Test
    void findsServersIgnoringCase() {
        service.add(root.resolve("srv"), id(), "my-server", 21);

        assertTrue(service.getServer("MY-SERVER").isPresent());
        assertTrue(service.getServer("nope").isEmpty());
    }

    @Test
    void listsOnlyServerProfiles() {
        service.add(root.resolve("srv"), id(), "my-server", 21);
        profileService.createDefault("client", VersionArg.parse("vanilla", "1.21.1"));

        assertEquals(List.of("my-server"), service.listServers().stream().map(Profile::name).toList());
    }

    @Test
    void savesAndRemovesServers() {
        Profile server = service.add(root.resolve("srv"), id(), "my-server", 21);

        service.save(server.withJavaVersion(17));
        assertEquals(17, service.getServer("my-server").orElseThrow().javaVersion());

        service.remove(server);
        assertEquals(List.of(), service.listServers());
    }

    @Test
    void eulaStatusIsOnlySavedWhenItChanges() {
        Profile server = service.add(root.resolve("srv"), id(), "my-server", 21);
        assertEquals(EulaStatus.UNKNOWN, server.eulaStatus());

        assertSame(server, service.setEulaStatus(server, EulaStatus.UNKNOWN));

        Profile updated = service.setEulaStatus(server, EulaStatus.EXISTS);
        assertEquals(EulaStatus.EXISTS, updated.eulaStatus());
        assertEquals(EulaStatus.EXISTS, service.getServer("my-server").orElseThrow().eulaStatus());
    }

}
