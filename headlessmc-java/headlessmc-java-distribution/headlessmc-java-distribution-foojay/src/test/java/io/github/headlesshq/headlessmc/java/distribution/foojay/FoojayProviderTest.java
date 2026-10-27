package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.ArchiveType;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionException;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FoojayProviderTest {
    private static final OS LINUX = new OS("linux", OS.Type.LINUX, "6.0");
    private static final URI DOWNLOAD_URI = URI.create("https://example.com/jdk-21.zip");

    /** A {@link FoojayAPI} that answers from the fields of this test instead of over HTTP. */
    private class TestApi implements FoojayAPI {
        @Override
        public Response getDistributions(Boolean includeVersions, Boolean includeSynonyms, List<String> scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response getDistributionByName(String distroName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response getPackages(String version, List<String> distribution, List<String> operatingSystem,
                                    List<String> architecture, Integer bitness, List<ArchiveType> archiveType,
                                    String packageType, Boolean directlyDownloadable, List<String> libcType,
                                    String latest, List<String> releaseStatus, Boolean javafxBundled) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response getPkgInfo(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Distribution> getDistributions() {
            return distributions;
        }

        @Override
        public Distribution getDistribution(String distroName) {
            return distributions.stream()
                .filter(distribution -> distribution.api_parameter().equals(distroName))
                .findFirst()
                .orElseThrow(() -> new ApiException("Failed to find Distribution " + distroName));
        }

        @Override
        public List<Pkg> packages(String version, List<String> distribution, List<String> operatingSystem,
                                  List<String> architecture, Integer bitness, List<ArchiveType> archiveType,
                                  String packageType, Boolean directlyDownloadable, List<String> libcType,
                                  String latest, List<String> releaseStatus, Boolean javafxBundled) {
            packageQueries.add(new PackageQuery(
                version, distribution, operatingSystem, architecture, bitness, latest, releaseStatus, javafxBundled
            ));
            return packages;
        }

        @Override
        public PkgInfo pkgInfo(String id) {
            if (pkgInfo == null) {
                throw new ApiException("Failed to find package info " + id);
            }

            return pkgInfo;
        }
    }

    private record PackageQuery(
        String version, List<String> distribution, List<String> operatingSystem,
        List<String> architecture, Integer bitness, String latest, List<String> releaseStatus, Boolean javafxBundled
    ) {}

    @TempDir
    Path root;

    private final List<Distribution> distributions = new ArrayList<>(List.of(new Distribution("Temurin", "temurin")));
    private final List<Pkg> packages = new ArrayList<>();
    private final List<PackageQuery> packageQueries = new ArrayList<>();

    private PkgInfo pkgInfo;

    private MockDownloadService downloads;
    private FoojayProvider provider;

    @BeforeEach
    void setup() {
        downloads = new MockDownloadService();
        provider = new FoojayProvider(
            Installers.installer(LINUX), downloads, new TestApi()
        );
    }

    @Test
    void nameIsTheDefaultProvider() {
        assertEquals(JavaDistributionProvider.DEFAULT, provider.getName());
    }

    @Test
    void listsDistributions() {
        assertEquals(
            List.of(new JavaDistribution(provider.getName(), "Temurin", "temurin")), provider.getDistributions()
        );
    }

    @Test
    void resolvesDistributionByName() {
        assertEquals("Temurin", provider.getDistributionByName("temurin").name());
    }

    @Test
    void unknownDistributionThrows() {
        assertThrows(JavaDistributionException.class, () -> provider.getDistributionByName("nope"));
    }

    @Test
    void listsRuntimesForTheGivenPlatform() {
        packages.add(new Pkg("pkg-id", "OpenJDK21U-jre_x64_linux.zip", 100L, "zip", "21.0.5+11", "jre"));

        List<JavaRuntime> runtimes = provider.getRuntimes(
            new JavaDistribution(provider.getName(), "Temurin", "temurin"), 21, LINUX, CPU.X64
        );

        assertEquals(1, runtimes.size());
        assertEquals("OpenJDK21U-jre_x64_linux", runtimes.getFirst().name());

        PackageQuery query = packageQueries.getFirst();
        assertEquals("21", query.version());
        assertEquals(List.of("temurin"), query.distribution());
        assertEquals(List.of("linux"), query.operatingSystem());
        assertEquals(List.of("x64"), query.architecture());
        assertEquals(64, query.bitness());
        assertEquals(FoojayProvider.LATEST_AVAILABLE, query.latest());
        assertEquals(List.of(FoojayProvider.RELEASE_STATUS_GA), query.releaseStatus());
        assertEquals(false, query.javafxBundled());
    }

    @Test
    void updateIsTheLatestRuntimeOfTheSamePackageType() {
        packages.add(new Pkg("latest-jdk", "latest-jdk.zip", 1L, "zip", "21.0.7+6", "jdk"));
        packages.add(new Pkg("latest", "latest.zip", 1L, "zip", "21.0.7+6", "jre"));

        Optional<JavaRuntime> update = provider.getUpdate(installed("21.0.5+11", "jre"), LINUX, CPU.X64);

        assertEquals(Optional.of("latest"), update.map(JavaRuntime::id));
        assertEquals(List.of("temurin"), packageQueries.getFirst().distribution());
        assertEquals("21", packageQueries.getFirst().version());
    }

    @Test
    void updateOfAnyPackageTypeIfUnknown() {
        packages.add(new Pkg("latest-jdk", "latest-jdk.zip", 1L, "zip", "21.0.6+7", "jdk"));

        Optional<JavaRuntime> update = provider.getUpdate(installed("21.0.5+11", null), LINUX, CPU.X64);

        assertEquals(Optional.of("latest-jdk"), update.map(JavaRuntime::id));
    }

    @Test
    void updateIfInstalledVersionIsUnknown() {
        packages.add(new Pkg("latest", "latest.zip", 1L, "zip", "21.0.5+11", "jre"));
        assertTrue(provider.getUpdate(installed(null, "jre"), LINUX, CPU.X64).isPresent());
    }

    @Test
    void noUpdateIfLatest() {
        packages.add(new Pkg("same", "same.zip", 1L, "zip", "21.0.5+11", "jre"));
        assertEquals(Optional.empty(), provider.getUpdate(installed("21.0.5+11", "jre"), LINUX, CPU.X64));
    }

    @Test
    void noUpdateWithoutRuntimes() {
        assertEquals(Optional.empty(), provider.getUpdate(installed("21.0.5+11", "jre"), LINUX, CPU.X64));
    }

    @Test
    void downloadsAndInstallsARuntime() throws Exception {
        byte[] archive = Archives.zip(Map.of("jdk-21/bin/java", "binary"));
        downloads.register(DOWNLOAD_URI, archive);
        pkgInfo = new PkgInfo(DOWNLOAD_URI.toString(), null, null, "jdk-21.zip");

        Path target = root.resolve("java-21");
        provider.download(runtime("zip"), target);

        assertTrue(downloads.wasRequested(DOWNLOAD_URI));
        assertEquals("binary", Files.readString(target.resolve("jdk-21").resolve("bin").resolve("java")));
    }

    @Test
    void downloadWithoutDownloadUriThrows() {
        pkgInfo = new PkgInfo(null, null, null, "jdk-21.zip");

        JavaDistributionException e = assertThrows(
            JavaDistributionException.class, () -> provider.download(runtime("zip"), root.resolve("java-21"))
        );
        assertTrue(e.getMessage().contains("Failed to get download URL"));
    }

    @Test
    void downloadWithoutArchiveTypeThrows() {
        pkgInfo = new PkgInfo(DOWNLOAD_URI.toString(), null, null, "jdk-21.zip");

        assertThrows(JavaDistributionException.class,
            () -> provider.download(runtime(null), root.resolve("java-21")));
        assertThrows(JavaDistributionException.class, () -> provider.download(
            new JavaRuntime(provider.getName(), "temurin", "pkg-id", "jdk-21", 21, null), root.resolve("java-21")
        ));
    }

    @Test
    void apiFailureIsWrapped() {
        pkgInfo = null;
        assertThrows(JavaDistributionException.class,
            () -> provider.download(runtime("zip"), root.resolve("java-21")));
    }

    private JavaRuntime runtime(String archiveType) {
        return new JavaRuntime(
            provider.getName(), "temurin", "pkg-id", "jdk-21", 21, new JavaRuntime.MetaData(null, archiveType, null, null)
        );
    }

    private JavaRuntime installed(String javaVersion, String packageType) {
        return new JavaRuntime(
            provider.getName(), "temurin", "pkg-id", "jdk-21", 21, new JavaRuntime.MetaData(null, "zip", javaVersion, packageType)
        );
    }

}
