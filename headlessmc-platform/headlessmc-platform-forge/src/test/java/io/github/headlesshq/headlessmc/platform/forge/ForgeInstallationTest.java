package io.github.headlesshq.headlessmc.platform.forge;

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
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.DefaultVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherServiceImpl;
import io.github.headlesshq.headlessmc.platform.util.CommonInstallerServices;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ForgeInstallationTest {
    private static final String REPOSITORY = "https://maven.minecraftforge.net";
    private static final byte[] INSTALLER = "forge-installer".getBytes(StandardCharsets.UTF_8);

    /** Records the command instead of running the forge installer. */
    private final class RecordingBuilder extends AbstractJavaProcessBuilder {
        @Override
        public JavaProcess start() {
            builders.add(this);
            return new JavaProcessImpl(
                getId(), Map.of(), getClassPath(), List.of(), getArgs(),
                Optional.of(new Java("j", 21, new SafePath(null), new SafePath(null), false, 0)),
                Optional.empty(), Optional.ofNullable(getMainClass()), Optional.ofNullable(getDirectory()),
                Optional.ofNullable(getJar()), Optional.empty()
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
    private FakePlatformService platformService;
    private FakeVersionJsonService versions;
    private Cache<PrismIndex> cache;
    private CommonInstallerServices services;
    private ForgeInstallerDownloader installerDownloader;
    private ForgeCLIInstaller cliInstaller;

    @BeforeEach
    void setup() {
        appFiles = TestFiles.appFiles(root);
        FakeVanillaVersionService vanillaVersions = new FakeVanillaVersionService("1.20.2");
        FakeVersionService forgeVersions = new FakeVersionService(Forge.PLATFORM_NAME)
            .withBuilds("1.20.2", "48.1.0");

        FakePlatform forge = new FakePlatform(Forge.PLATFORM_NAME, forgeVersions);
        forge.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, forgeVersions),
            (id, mcDir, args) -> new FakeVersion(id.getVersion().getName())
        ));

        platformService = new FakePlatformService(new FakeVanillaPlatform("1.20.2"), forge);
        versions = new FakeVersionJsonService();

        cache = CacheBuilder.<PrismIndex>create()
            .withSource(() -> Optional.of(new PrismIndex("net.minecraftforge", List.of(
                new PrismIndex.Meta(
                    List.of(new PrismIndex.Meta.Requires("1.20.2", "net.minecraft")), "sha", "48.1.0"
                )
            ))))
            .build();

        services = new CommonInstallerServices(
            vanillaVersions, new VersionMatcherServiceImpl(platformService), launcherService, versions,
            vanillaInstaller(), downloads, new DefaultFileService(new DefaultFileSystemProvider()), appFiles
        );

        installerDownloader = new ForgeInstallerDownloader(
            new ForgeArtifactResolverImpl(), services, MavenRepository.of(REPOSITORY), cache, Forge.PLATFORM_NAME
        );
        cliInstaller = new ForgeCLIInstaller(launcherService, new DefaultFileService(new DefaultFileSystemProvider()));
        downloads.register(installerUrl(), INSTALLER);
    }

    private URI installerUrl() {
        return MavenRepository.of(REPOSITORY)
            .getJar(new Artifact("net.minecraftforge", "forge", "1.20.2-48.1.0", "installer"));
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

    private ForgeInstaller installer() {
        return new ForgeInstaller(installerDownloader, services, cliInstaller, cache, Forge.PLATFORM_NAME);
    }

    @Test
    void installsAServerWithTheForgeInstaller() {
        Path dir = root.resolve("srv");

        var installation = installer().installServer(id("forge", "1.20.2", "48.1.0"), dir, new TypedMapImpl());

        assertEquals(21, installation.javaVersion());
        RecordingBuilder builder = builders.getFirst();
        assertEquals("forge-installer", builder.getId());
        assertEquals(dir, builder.getDirectory());
        assertEquals(List.of("--installServer", dir.toAbsolutePath().toString()), builder.getArgs());
        // the downloaded installer is deleted afterwards
        assertFalse(Files.exists(builder.getJar()));
    }

    @Test
    void installsAClientThroughTheCliInstaller() throws Exception {
        Path mcDir = root.resolve("mc");
        ForgeInstaller installer = new ForgeInstaller(
            installerDownloader, services, cliInstaller, cache, Forge.PLATFORM_NAME
        ) {
            @Override
            protected void installClient(VersionID id, Path mcDir, Version vanilla, TypedMap args) {
                super.installClient(id, mcDir, vanilla, args);
                versions.add(new FakeVersion("1.20.2-forge-48.1.0"));
            }
        };

        Version version = installer.installClient(id("forge", "1.20.2", "48.1.0"), mcDir, new TypedMapImpl());

        assertEquals("1.20.2-forge-48.1.0", version.getId());
        RecordingBuilder builder = builders.getFirst();
        assertEquals("forge-cli-installer", builder.getId());
        assertEquals(ForgeCLIInstaller.MAIN_CLASS, builder.getMainClass());
        assertEquals(2, builder.getClassPath().size());
        assertTrue(Files.exists(mcDir.resolve("launcher_profiles.json")));
        assertTrue(Files.exists(mcDir.resolve("launcher_profiles_microsoft_store.json")));
        assertEquals(
            "{\"profiles\": {}}", Files.readString(mcDir.resolve("launcher_profiles.json"))
        );
    }

    @Test
    void existingLauncherProfilesAreKept() throws Exception {
        Path mcDir = Files.createDirectories(root.resolve("mc"));
        Files.writeString(mcDir.resolve("launcher_profiles.json"), "{\"profiles\": {\"a\": {}}}");

        cliInstaller.installClient(root.resolve("installer.jar"), mcDir, 21);

        assertEquals("{\"profiles\": {\"a\": {}}}", Files.readString(mcDir.resolve("launcher_profiles.json")));
    }

    @Test
    void installingForAnotherPlatformIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> installer().installClient(
            id("vanilla", "1.20.2"), root.resolve("mc"), new TypedMapImpl()
        ));
    }

}
