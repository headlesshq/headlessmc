package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractClientInstallerTest {
    private final List<VersionID> vanillaInstalls = new ArrayList<>();
    private final List<VersionID> clientInstalls = new ArrayList<>();

    private FakePlatformService platformService;
    private FakeVersionJsonService versions;
    private VersionMatcherServiceImpl matcherService;
    private TestInstaller installer;

    /** The version the installer registers when it "installs" something. */
    private Version installedVersion = new FakeVersion("fabric-loader-0.16.9-1.21.1");

    private final VanillaInstaller vanillaInstaller = new VanillaInstaller() {
        @Override
        public Version getVersion(VanillaVersion version) {
            return new FakeVersion(version.getName());
        }

        @Override
        public Version installClient(VersionID id, Path mcDir, TypedMap args) {
            vanillaInstalls.add(id);
            return new FakeVersion(id.getVersion().getName());
        }

        @Override
        public Installation installServer(VersionID id, Path dir, TypedMap args) {
            throw new UnsupportedOperationException();
        }
    };

    private class TestInstaller extends AbstractClientInstaller {
        @Override
        protected void installClient(VersionID id, Path mcDir, Version vanilla, TypedMap args) {
            clientInstalls.add(id);
            versions.add(installedVersion);
        }

        @Override
        protected InstallerServices getServices() {
            return new InstallerServices() {
                @Override
                public VersionMatcherService getVersionMatcherService() {
                    return matcherService;
                }

                @Override
                public VersionJsonService getVersionService() {
                    return versions;
                }

                @Override
                public VanillaInstaller getVanillaInstaller() {
                    return vanillaInstaller;
                }
            };
        }

        @Override
        protected String getPlatformName() {
            return "fabric";
        }
    }

    @BeforeEach
    void setup() {
        FakeVanillaVersionService vanillaVersions = new FakeVanillaVersionService("1.21.1", "1.20.4");
        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");

        FakeVersionService fabricVersions = new FakeVersionService("fabric").withBuilds("1.21.1", "0.16.9");
        FakePlatform fabric = new FakePlatform("fabric", fabricVersions);
        fabric.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, fabricVersions),
            (id, mcDir, args) -> new FakeVersion(id.getVersion().getName())
        ));

        platformService = new FakePlatformService(vanilla, fabric);
        versions = new FakeVersionJsonService();
        matcherService = new VersionMatcherServiceImpl(platformService);
        installer = new TestInstaller();
    }

    private VersionID id(String... arg) {
        return VersionID.resolve(platformService, VersionArg.parse(arg));
    }

    @Test
    void installingForAnotherPlatformThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> installer.installClient(id("vanilla", "1.21.1"), Path.of("mc"), new TypedMapImpl()));
        assertTrue(e.getMessage().contains("installer for platform fabric"));
    }

    @Test
    void installsVanillaAndClientAndMatchesTheResult() {
        Version result = installer.installClient(id("fabric", "1.21.1"), Path.of("mc"), new TypedMapImpl());

        assertEquals(installedVersion, result);
        assertEquals(1, vanillaInstalls.size());
        assertEquals(1, clientInstalls.size());
    }

    @Test
    void alreadyInstalledVersionIsReused() {
        versions.add(installedVersion);

        Version result = installer.installClient(id("fabric", "1.21.1"), Path.of("mc"), new TypedMapImpl());

        assertEquals(installedVersion, result);
        assertEquals(List.of(), clientInstalls);
    }

    @Test
    void forceInstallReinstallsAnExistingVersion() {
        versions.add(installedVersion);
        installedVersion = new FakeVersion("fabric-loader-0.16.9-1.21.1-fresh");

        TypedMap args = new TypedMapImpl();
        args.put(ClientInstaller.FORCE_INSTALL, true);
        Version result = installer.installClient(id("fabric", "1.21.1"), Path.of("mc"), args);

        assertEquals(1, clientInstalls.size());
        assertEquals("fabric-loader-0.16.9-1.21.1-fresh", result.getId());
    }

    @Test
    void installerThatInstallsNothingThrows() {
        installedVersion = new FakeVersion("some-unrelated-version");

        assertThrows(VersionMatchException.class,
            () -> installer.installClient(id("fabric", "1.21.1"), Path.of("mc"), new TypedMapImpl()));
    }

}
