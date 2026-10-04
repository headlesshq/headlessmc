package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.util.CommonInstallerServices;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ForgeInstallerDownloaderTest {
    private static final String REPOSITORY = "https://maven.minecraftforge.net";
    private static final byte[] INSTALLER = "installer-jar".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path root;

    private final MockDownloadService downloads = new MockDownloadService();
    private final List<PrismIndex.Meta> metas = new ArrayList<>();

    private AppFiles appFiles;
    private FakePlatformService platformService;
    private ForgeInstallerDownloader downloader;

    @BeforeEach
    void setup() {
        appFiles = TestFiles.appFiles(root);
        metas.add(meta("48.1.0", "1.20.2"));
        metas.add(meta("11.15.1.2318-1.8.9", "1.8.9"));

        Cache<PrismIndex> cache = CacheBuilder.<PrismIndex>create()
            .withSource(() -> Optional.of(new PrismIndex("net.minecraftforge", metas)))
            .build();

        // the downloader only reaches for these three of the eight common services
        CommonInstallerServices services = Mockito.mock(CommonInstallerServices.class);
        Mockito.when(services.getDownloadService()).thenReturn(downloads);
        Mockito.when(services.getFileService()).thenReturn(new DefaultFileService(new DefaultFileSystemProvider()));
        Mockito.when(services.getAppFiles()).thenReturn(appFiles);

        downloader = new ForgeInstallerDownloader(
            new ForgeArtifactResolverImpl(), services, MavenRepository.of(REPOSITORY), cache, Forge.PLATFORM_NAME
        );

        FakePlatform forge = FakePlatform.create(
            Forge.PLATFORM_NAME, new String[]{"1.20.2", "48.1.0"}, new String[]{"1.8.9", "11.15.1.2318-1.8.9"}
        );
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.20.2", "1.8.9"), forge);
    }

    private static PrismIndex.Meta meta(String version, String mcVersion) {
        return new PrismIndex.Meta(
            List.of(new PrismIndex.Meta.Requires(mcVersion, "net.minecraft")), "sha", version
        );
    }

    private VersionID id(String... arg) {
        return VersionID.resolve(platformService, VersionArg.parse(arg));
    }

    private URI url(String version) {
        return MavenRepository.of(REPOSITORY)
            .getJar(new Artifact("net.minecraftforge", "forge", version, "installer"));
    }

    private Path cachePath(String version) {
        return new Artifact("net.minecraftforge", "forge", version, "installer")
            .jar(appFiles.getCacheDir().resolve("forge"));
    }

    @Test
    void downloadsTheInstallerForAVersion() throws Exception {
        downloads.register(url("1.20.2-48.1.0"), INSTALLER);

        Path path = downloader.downloadInstaller(id("forge", "1.20.2", "48.1.0"));

        assertEquals(cachePath("1.20.2-48.1.0"), path);
        assertArrayEquals(INSTALLER, Files.readAllBytes(path));
    }

    @Test
    void anAlreadyDownloadedInstallerIsReused() throws Exception {
        Path path = cachePath("1.20.2-48.1.0");
        Files.createDirectories(path.getParent());
        Files.write(path, INSTALLER);

        assertEquals(path, downloader.downloadInstaller(id("forge", "1.20.2", "48.1.0")));
        assertFalse(downloads.wasRequested(url("1.20.2-48.1.0")));
    }

    @Test
    void theLatestMatchingMetaIsUsedWithoutABuild() throws Exception {
        downloads.register(url("1.20.2-48.1.0"), INSTALLER);

        assertEquals(cachePath("1.20.2-48.1.0"), downloader.downloadInstaller(id("forge", "1.20.2")));
    }

    @Test
    void aFailedDownloadRetriesWithTheMcVersionSuffix() throws Exception {
        // the plain artifact is not registered, the "-<mcVersion>" variant is
        downloads.register(url("1.8.9-11.15.1.2318-1.8.9-1.8.9"), INSTALLER);

        Path path = downloader.downloadInstaller(id("forge", "1.8.9", "11.15.1.2318-1.8.9"));

        assertEquals(cachePath("1.8.9-11.15.1.2318-1.8.9-1.8.9"), path);
        assertArrayEquals(INSTALLER, Files.readAllBytes(path));
    }

    @Test
    void aCachedFallbackArtifactIsReused() throws Exception {
        Path path = cachePath("1.8.9-11.15.1.2318-1.8.9-1.8.9");
        Files.createDirectories(path.getParent());
        Files.write(path, INSTALLER);

        assertEquals(path, downloader.downloadInstaller(id("forge", "1.8.9", "11.15.1.2318-1.8.9")));
    }

    @Test
    void unknownVersionsThrow() {
        metas.clear();
        metas.add(meta("48.1.0", "1.7.10"));

        assertThrows(NotFoundException.class, () -> downloader.downloadInstaller(id("forge", "1.20.2")));
    }

    @Test
    void ambiguousBuildsThrow() {
        metas.clear();
        metas.add(meta("48.1.0-a", "1.20.2"));
        metas.add(meta("48.1.0-b", "1.20.2"));

        FakePlatform forge = FakePlatform.create(Forge.PLATFORM_NAME, new String[]{"1.20.2", "48.1.0"});
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.20.2"), forge);

        // both metas contain the requested build and have the same length
        assertThrows(NotFoundException.class, () -> downloader.downloadInstaller(id("forge", "1.20.2", "48.1.0")));
    }

}
