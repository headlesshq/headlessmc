package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.purpur.api.BuildResponse;
import io.github.headlesshq.headlessmc.platform.purpur.api.ProjectResponse;
import io.github.headlesshq.headlessmc.platform.purpur.api.PurpurAPI;
import io.github.headlesshq.headlessmc.platform.purpur.api.VersionResponse;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurpurInstallerTest {
    @TempDir
    Path root;

    private final MockDownloadService downloads = new MockDownloadService();

    private final VanillaInstaller vanillaInstaller = new VanillaInstaller() {
        @Override
        public Version getVersion(VanillaVersion version) {
            return new FakeVersion(version.getName()).withJavaVersion(21);
        }

        @Override
        public Version installClient(VersionID id, Path mcDir, TypedMap args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Installation installServer(VersionID id, Path dir, TypedMap args) {
            throw new UnsupportedOperationException();
        }
    };

    private final PurpurAPI api = new PurpurAPI() {
        @Override
        public ProjectResponse getProject(String project) {
            return new ProjectResponse(project, List.of("1.21.1"));
        }

        @Override
        public VersionResponse getVersion(String project, String version) {
            return new VersionResponse(version, new VersionResponse.Builds("2329", List.of("2328", "2329")));
        }

        @Override
        public BuildResponse getBuild(String project, String version, String build) {
            return new BuildResponse(project, version, build, md5("jar-" + build));
        }
    };

    private FakePlatformService platformService;
    private PurpurInstaller installer;

    @BeforeEach
    void setup() {
        FakePlatform purpur = FakePlatform.create(Purpur.PLATFORM_NAME, new String[]{"1.21.1", "2329", "2328"});
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"), purpur);
        installer = new PurpurInstaller(downloads, vanillaInstaller, api);

        downloads.register(url("2329"), "jar-2329");
        downloads.register(url("2328"), "jar-2328");
    }

    private URI url(String build) {
        return URI.create(PurpurAPI.V2_URL + "/purpur/1.21.1/" + build + "/download");
    }

    private static String md5(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void installsTheLatestBuildWhenNoBuildIsGiven() throws Exception {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("purpur", "1.21.1"));

        ServerInstaller.Installation installation = installer.installServer(id, root, new TypedMapImpl());

        assertEquals(21, installation.javaVersion());
        assertEquals("jar-2329", Files.readString(root.resolve(ServerFinder.DEFAULT_JAR)));
        assertTrue(downloads.wasRequested(url("2329")));
    }

    @Test
    void installsTheGivenBuild() throws Exception {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("purpur", "1.21.1", "2328"));

        installer.installServer(id, root, new TypedMapImpl());

        assertEquals("jar-2328", Files.readString(root.resolve(ServerFinder.DEFAULT_JAR)));
    }

}
