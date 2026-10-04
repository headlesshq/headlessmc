package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.java.launcher.AbstractJavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessImpl;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.DefaultVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherServiceImpl;
import io.github.headlesshq.headlessmc.platform.util.CommonInstallerServices;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FabricInstallationTest {
    private static final byte[] INSTALLER = "fabric-installer".getBytes(StandardCharsets.UTF_8);
    private static final String REPOSITORY = "https://maven.fabricmc.net";
    private static final String LEGACY_REPOSITORY = "https://maven.legacyfabric.net";

    /** Records the command instead of running the fabric installer. */
    private final class RecordingBuilder extends AbstractJavaProcessBuilder {
        @Override
        public JavaProcess start() {
            builders.add(this);
            return new JavaProcessImpl(
                getId(), Map.of(), List.of(), List.of(), getArgs(),
                Optional.of(new Java("j", 21, new SafePath(null), new SafePath(null), false, 0)),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(getJar()), Optional.empty()
            );
        }
    }

    @TempDir
    Path root;

    private final List<RecordingBuilder> builders = new ArrayList<>();
    private final MockDownloadService downloads = new MockDownloadService();

    private final JavaLauncherService launcherService = new JavaLauncherService() {
        @Override
        public JavaProcessBuilder buildProcess() {
            return new RecordingBuilder();
        }

        @Override
        public String getName() {
            return "recording";
        }
    };

    private AppFiles appFiles;
    private FakeVanillaVersionService vanillaVersions;
    private FakePlatformService platformService;
    private FakeVersionJsonService versions;
    private InstallerArtifactService artifactService;
    private FabricInstallerDownloader installerDownloader;
    private CommonInstallerServices services;

    private static String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private InstallerArtifact artifact(String repository, String version) {
        return new InstallerArtifact(
            "net.fabricmc", "fabric-installer", version, "fabric-installer-" + version + ".jar",
            INSTALLER.length, sha256(INSTALLER), repository
        );
    }

    @BeforeEach
    void setup() {
        appFiles = TestFiles.appFiles(root);
        vanillaVersions = new FakeVanillaVersionService("1.21.1", "1.14", "1.12.2");

        FakeVersionService fabricVersions = new FakeVersionService(Fabric.PLATFORM_NAME)
            .withBuilds("1.21.1", "0.16.9");
        FakePlatform fabric = new FakePlatform(Fabric.PLATFORM_NAME, fabricVersions);
        fabric.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, fabricVersions),
            (id, mcDir, args) -> new FakeVersion(id.getVersion().getName())
        ));

        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1", "1.14", "1.12.2");
        platformService = new FakePlatformService(vanilla, fabric);
        versions = new FakeVersionJsonService();

        artifactService = new TestArtifactService();
        services = new CommonInstallerServices(
            vanillaVersions, new VersionMatcherServiceImpl(platformService), launcherService, versions,
            vanillaInstaller(), downloads, new DefaultFileService(new DefaultFileSystemProvider()), appFiles
        );

        installerDownloader = new FabricInstallerDownloader(
            vanillaVersions, downloads, new DefaultFileService(new DefaultFileSystemProvider()), appFiles, artifactService
        );
    }

    /** Serves fixed {@link InstallerArtifact}s instead of reading the generated resource. */
    private final class TestArtifactService extends InstallerArtifactService {
        TestArtifactService() {
            super(new DefaultJacksonJsonService());
        }

        @Override
        public InstallerArtifact getInstaller() {
            return artifact(REPOSITORY, "1.1.1");
        }

        @Override
        public InstallerArtifact getLegacyInstaller() {
            return artifact(LEGACY_REPOSITORY, "1.1.1-legacy");
        }
    }

    private VanillaInstaller vanillaInstaller() {
        return new VanillaInstaller() {
            @Override
            public Version getVersion(VanillaVersion version) {
                return new FakeVersion(version.getName()).withJavaVersion(21);
            }

            @Override
            public Version installClient(VersionID id, Path mcDir, TypedMap args) {
                return new FakeVersion(id.getVersion().getName()).withJavaVersion(21);
            }

            @Override
            public Installation installServer(VersionID id, Path dir, TypedMap args) {
                return new Installation(21);
            }
        };
    }

    private VersionID id(String... arg) {
        return VersionID.resolve(platformService, VersionArg.parse(arg));
    }

    private Path installerPath(InstallerArtifact artifact) {
        return artifact.artifact().jar(appFiles.getCacheDir().resolve("fabric").resolve("installer"));
    }

    // ------------------------------------------------- FabricInstallerDownloader

    @Test
    void downloadsTheModernInstallerForNewVersions() throws Exception {
        InstallerArtifact artifact = artifactService.getInstaller();
        downloads.register(artifact.getURL(), INSTALLER);

        Path path = installerDownloader.download(id("fabric", "1.21.1"), null);

        assertEquals(installerPath(artifact), path);
        assertArrayEquals(INSTALLER, Files.readAllBytes(path));
    }

    @Test
    void downloadsTheLegacyInstallerForOldVersions() throws Exception {
        InstallerArtifact artifact = artifactService.getLegacyInstaller();
        downloads.register(artifact.getURL(), INSTALLER);

        Path path = installerDownloader.download(id("fabric", "1.12.2"), null);

        assertEquals(installerPath(artifact), path);
    }

    @Test
    void anAlreadyDownloadedInstallerIsReused() throws Exception {
        InstallerArtifact artifact = artifactService.getInstaller();
        Path path = installerPath(artifact);
        Files.createDirectories(path.getParent());
        Files.write(path, INSTALLER);

        assertEquals(path, installerDownloader.download(artifact));
        assertFalse(downloads.wasRequested(artifact.getURL()));
    }

    @Test
    void aCustomInstallerUrlIsUsedAsIs() throws Exception {
        URI custom = URI.create("https://example.com/custom-installer.jar");
        downloads.register(custom, INSTALLER);

        Path path = installerDownloader.download(id("fabric", "1.21.1"), custom);

        assertEquals(appFiles.getCacheDir().resolve("fabric-custom-installer.jar"), path);
        assertArrayEquals(INSTALLER, Files.readAllBytes(path));
    }

    @Test
    void installerArtifactsBuildTheirMavenUrl() {
        InstallerArtifact artifact = artifact(REPOSITORY, "1.1.1");

        assertEquals(
            URI.create(REPOSITORY + "/net/fabricmc/fabric-installer/1.1.1/fabric-installer-1.1.1.jar"),
            artifact.getURL()
        );
        assertEquals(new Artifact("net.fabricmc", "fabric-installer", "1.1.1"), artifact.artifact());
    }

    // -------------------------------------------------------- FabricInstaller

    private FabricInstaller installer() {
        return new FabricInstaller(installerDownloader, services, Fabric.PLATFORM_NAME);
    }

    @Test
    void installsAClientWithTheFabricInstaller() {
        downloads.register(artifactService.getInstaller().getURL(), INSTALLER);
        versions.add(new FakeVersion("fabric-loader-0.16.9-1.21.1"));

        Version version = installer().installClient(
            id("fabric", "1.21.1", "0.16.9"), root.resolve("mc"), new TypedMapImpl()
        );

        // the version was already installed, so no installer process was needed
        assertEquals("fabric-loader-0.16.9-1.21.1", version.getId());
        assertEquals(List.of(), builders);
    }

    @Test
    void runsTheInstallerForANewClient() {
        downloads.register(artifactService.getInstaller().getURL(), INSTALLER);
        FabricInstaller installer = new FabricInstaller(installerDownloader, services, Fabric.PLATFORM_NAME) {
            @Override
            protected void installClient(VersionID id, Path mcDir, Version vanilla, TypedMap args) {
                super.installClient(id, mcDir, vanilla, args);
                versions.add(new FakeVersion("fabric-loader-0.16.9-1.21.1"));
            }
        };

        Version version = installer.installClient(
            id("fabric", "1.21.1", "0.16.9"), root.resolve("mc"), new TypedMapImpl()
        );

        assertEquals("fabric-loader-0.16.9-1.21.1", version.getId());
        RecordingBuilder builder = builders.getFirst();
        assertEquals("fabric-installer", builder.getId());
        assertEquals(21, builder.getVersion());
        assertEquals(
            List.of("client", "-noprofile", "-mcversion", "1.21.1", "-loader", "0.16.9",
                "-dir", root.resolve("mc").toAbsolutePath().toString()),
            builder.getArgs()
        );
    }

    @Test
    void installsAServerWithTheFabricInstaller() {
        downloads.register(artifactService.getInstaller().getURL(), INSTALLER);

        var installation = installer().installServer(
            id("fabric", "1.21.1", "0.16.9"), root.resolve("srv"), new TypedMapImpl()
        );

        assertEquals(21, installation.javaVersion());
        assertEquals("server", builders.getFirst().getArgs().getFirst());
        assertFalse(builders.getFirst().getArgs().contains("-noprofile"));
    }

    @Test
    void installingForAnotherPlatformIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> installer().installClient(
            id("vanilla", "1.21.1"), root.resolve("mc"), new TypedMapImpl()
        ));
    }

    @Test
    void aCustomInstallerUrlIsPassedThrough() throws Exception {
        URI custom = URI.create("https://example.com/custom-installer.jar");
        downloads.register(custom, INSTALLER);

        TypedMap args = new TypedMapImpl();
        args.put(ClientInstaller.CUSTOM_INSTALLER_URL, custom);
        installer().installServer(id("fabric", "1.21.1", "0.16.9"), root.resolve("srv"), args);

        assertTrue(downloads.wasRequested(custom));
    }

    // ---------------------------------------------------- FabricVersionService

    @Test
    void versionServiceListsLoaderBuilds() {
        Cache<List<BuildData>> cache = CacheBuilder.<List<BuildData>>create()
            .withSource(() -> Optional.of(List.of(
                new BuildData(".", 3, "net.fabricmc:fabric-loader:0.16.9", "0.16.9", true),
                new BuildData(".", 2, "net.fabricmc:fabric-loader:0.16.5", "0.16.5", true)
            )))
            .build();

        FabricVersionService service = new FabricVersionService(cache);
        VanillaVersion version = vanillaVersions.getVersion("1.21.1").orElseThrow();

        assertEquals(
            List.of("0.16.9", "0.16.5"),
            service.getVersions().stream().map(PlatformVersion::getName).toList()
        );
        // on fabric every loader can load every mc version
        assertEquals(service.getVersions(), service.getBuilds(version));
    }

}
