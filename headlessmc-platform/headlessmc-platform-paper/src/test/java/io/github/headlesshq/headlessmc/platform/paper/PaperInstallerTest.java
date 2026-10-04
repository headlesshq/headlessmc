package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.paper.api.BuildResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.Channel;
import io.github.headlesshq.headlessmc.platform.paper.api.PaperAPI;
import io.github.headlesshq.headlessmc.platform.paper.api.ProjectResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.VersionResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.VersionsResponse;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaperInstallerTest {
    private static final URI URL_130 = URI.create("https://papermc.io/paper-130.jar");

    @TempDir
    Path root;

    private final MockDownloadService downloads = new MockDownloadService();

    private final VanillaInstaller vanillaInstaller = new VanillaInstaller() {
        @Override
        public Version getVersion(VanillaVersion version) {
            return new FakeVersion(version.getName()).withJavaVersion(21);
        }

        @Override
        public Version installClient(VersionID id, Path mcDir, io.github.headlesshq.headlessmc.util.typemap.TypedMap args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Installation installServer(VersionID id, Path dir, io.github.headlesshq.headlessmc.util.typemap.TypedMap args) {
            throw new UnsupportedOperationException();
        }
    };

    private final PaperAPI api = new PaperAPI() {
        @Override
        public ProjectResponse getProject(String project) {
            return new ProjectResponse(
                new ProjectResponse.Project(project, "Paper"),
                Map.of("26.1", List.of("26.1.2", "26.1.1"), "1.21", List.of("1.21.1"))
            );
        }

        @Override
        public VersionsResponse getVersions(String project) {
            return new VersionsResponse(List.of());
        }

        @Override
        public VersionResponse getVersion(String project, String version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BuildResponse getBuild(String project, String version, Integer build) {
            return build(build);
        }

        @Override
        public BuildResponse getLatest(String project, String version) {
            if (!"1.21.1".equals(version)) {
                throw new ApiException(Response.status(404).build());
            }

            return build(130);
        }

        @Override
        public List<BuildResponse> getBuilds(String project, String version, List<Channel> channel) {
            return List.of(build(130));
        }
    };

    private FakePlatformService platformService;
    private PaperInstaller installer;

    @BeforeEach
    void setup() {
        FakePlatform paper = FakePlatform.create(Paper.PLATFORM_NAME, new String[]{"1.21.1", "130", "129"});
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1", "26.1", "1.20.4"), paper);
        installer = new PaperInstaller(downloads, vanillaInstaller, api);
        downloads.register(URL_130, "jar-130");
        downloads.register(URI.create("https://papermc.io/paper-129.jar"), "jar-129");
    }

    private static BuildResponse build(int id) {
        return new BuildResponse(id, "STABLE", Map.of(
            BuildResponse.DEFAULT_DOWNLOAD,
            new BuildResponse.Download(
                "paper-" + id + ".jar",
                Map.of("sha256", sha256("jar-" + id)),
                ("jar-" + id).length(),
                "https://papermc.io/paper-" + id + ".jar"
            )
        ));
    }

    private static String sha256(String content) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void installsTheLatestBuildWhenNoBuildIsGiven() throws Exception {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("paper", "1.21.1"));

        ServerInstaller.Installation installation = installer.installServer(id, root, new TypedMapImpl());

        assertEquals(21, installation.javaVersion());
        assertEquals("jar-130", Files.readString(root.resolve(ServerFinder.DEFAULT_JAR)));
        assertTrue(downloads.wasRequested(URL_130));
    }

    @Test
    void installsTheGivenBuild() throws Exception {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("paper", "1.21.1", "129"));

        installer.installServer(id, root, new TypedMapImpl());

        assertEquals("jar-129", Files.readString(root.resolve(ServerFinder.DEFAULT_JAR)));
    }

    @Test
    void suggestsTheVersionsOfTheGroupIfPaperHasNoBuildsForTheVersion() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("paper", "26.1"));

        HeadlessMcException e = assertThrows(
            HeadlessMcException.class, () -> installer.installServer(id, root, new TypedMapImpl())
        );

        assertEquals("Paper has no builds for Minecraft 26.1. Available versions for 26.1: 26.1.2, 26.1.1", e.getMessage());
        assertFalse(Files.exists(root.resolve(ServerFinder.DEFAULT_JAR)));
    }

    @Test
    void reportsMissingVersionWithoutGroup() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("paper", "1.20.4"));

        HeadlessMcException e = assertThrows(
            HeadlessMcException.class, () -> installer.installServer(id, root, new TypedMapImpl())
        );

        assertEquals("Paper has no builds for Minecraft 1.20.4.", e.getMessage());
    }

}
