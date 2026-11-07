package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.distribution.InstalledJavaRuntimeService;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionException;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JModsServiceTest {
    private final InstalledJavaRuntimeService installedRuntimes =
        new InstalledJavaRuntimeService(new DefaultJacksonJsonService());

    @TempDir
    Path root;

    /** The runtimes each distribution offers. */
    private final Map<String, List<JavaRuntime>> runtimes = new HashMap<>();
    /** The ids of the runtimes whose download contains jmods. */
    private final List<String> withJMods = new ArrayList<>();
    private final List<JavaRuntime> downloads = new ArrayList<>();

    private List<String> distributions = List.of("temurin", "zulu");
    private boolean download = true;

    private final JavaDistributionProvider provider = new JavaDistributionProvider() {
        @Override
        public String getName() {
            return "foojay.io";
        }

        @Override
        public List<JavaDistribution> getDistributions() {
            return List.of();
        }

        @Override
        public JavaDistribution getDistributionByName(String name) {
            if (!runtimes.containsKey(name)) {
                throw new JavaDistributionException("Unknown distribution " + name);
            }

            return new JavaDistribution(getName(), name, name);
        }

        @Override
        public List<JavaRuntime> getRuntimes(JavaDistribution distribution, int version, OS os, CPU cpu) {
            return runtimes.get(distribution.id()).stream().filter(runtime -> runtime.version() == version).toList();
        }

        @Override
        public Optional<JavaRuntime> getUpdate(JavaRuntime runtime, OS os, CPU cpu) {
            return Optional.empty();
        }

        @Override
        public void download(JavaRuntime runtime, Path path) {
            downloads.add(runtime);
            // nested like on macOS
            Path home = path.resolve(runtime.name()).resolve("Contents").resolve("Home");
            write(home.resolve("bin").resolve("java"));
            if (withJMods.contains(runtime.id())) {
                write(home.resolve("jmods").resolve("java.base.jmod"));
                write(home.resolve("jmods").resolve("java.desktop.jmod"));
            }
        }
    };

    private final JavaDistributionProviderService providerService = new JavaDistributionProviderService() {
        @Override
        public List<JavaDistributionProvider> getProviders() {
            return List.of(provider);
        }

        @Override
        public Optional<JavaDistributionProvider> getProviderByName(String name) {
            return Optional.of(provider);
        }

        @Override
        public JavaDistributionProvider getDefaultProvider() {
            return provider;
        }
    };

    private AppFiles appFiles;
    private JModsService service;

    @BeforeEach
    void setup() {
        appFiles = TestFiles.appFiles(root.resolve("hmc"));
        Holder<JavaConfig> config = () -> new JavaConfig() {
            @Override
            public Optional<List<Path>> versions() {
                return Optional.empty();
            }

            @Override
            public boolean failOnParsingFailure() {
                return false;
            }

            @Override
            public boolean download() {
                return download;
            }

            @Override
            public int maxScanDepth() {
                return 6;
            }

            @Override
            public boolean checkForUpdates() {
                return false;
            }

            @Override
            public List<String> jmodsDistributions() {
                return distributions;
            }
        };

        service = new JModsService(providerService, installedRuntimes, config,
            new DefaultFileService(new DefaultFileSystemProvider()), appFiles, CPU.X64,
            new OS("linux", OS.Type.LINUX, "6.0"));
    }

    private static void write(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, file.getFileName().toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JavaRuntime runtime(String distribution, String packageType, int version) {
        String id = distribution + "-" + packageType + "-" + version;
        return new JavaRuntime("foojay.io", distribution, id, id, version,
            new JavaRuntime.MetaData(1L, "tar.gz", version + ".0.1", packageType));
    }

    private Java java(int version, Path home) {
        return new Java("java-" + version, version, new SafePath(home), new SafePath(home.resolve("bin/java")), false, 0);
    }

    @Test
    void theJmodsOfAnInstallationArePreferred() {
        Path home = root.resolve("jdk-21");
        write(home.resolve("jmods").resolve("java.base.jmod"));

        assertEquals(Optional.of(home.resolve("jmods")), service.findJMods(java(21, home)));
        assertTrue(downloads.isEmpty());
    }

    @Test
    void installationsWithoutJmodsFallBackToTheCache() {
        Java java = java(25, root.resolve("jdk-25"));
        assertEquals(Optional.empty(), service.findJMods(java));

        write(appFiles.getJModsDir().resolve("25").resolve("java.base.jmod"));
        assertEquals(Optional.of(appFiles.getJModsDir().resolve("25")), service.findJMods(java));
    }

    @Test
    void jmodsAreDownloadedFromTheFirstDistributionThatHasThem() throws Exception {
        // temurin 25 jdk ships without jmods (JEP 493), the jre is never a candidate
        runtimes.put("temurin", List.of(runtime("temurin", "jre", 25), runtime("temurin", "jdk", 25)));
        runtimes.put("zulu", List.of(runtime("zulu", "jdk", 25)));
        withJMods.add("zulu-jdk-25");

        Path jmods = service.getJMods(25);

        assertEquals(appFiles.getJModsDir().resolve("25"), jmods);
        assertTrue(Files.isRegularFile(jmods.resolve("java.base.jmod")));
        assertTrue(Files.isRegularFile(jmods.resolve("java.desktop.jmod")));
        // only the jmods are kept
        assertFalse(Files.exists(jmods.resolve("bin")));
        assertEquals(List.of("temurin-jdk-25", "zulu-jdk-25"), downloads.stream().map(JavaRuntime::id).toList());
        assertEquals(Optional.of("zulu-jdk-25"), installedRuntimes.read(jmods).map(JavaRuntime::id));

        // cached, no second download
        assertEquals(jmods, service.getJMods(25));
        assertEquals(2, downloads.size());
    }

    @Test
    void unknownDistributionsAreSkipped() {
        distributions = List.of("does-not-exist", "zulu");
        runtimes.put("zulu", List.of(runtime("zulu", "jdk", 24)));
        withJMods.add("zulu-jdk-24");

        assertTrue(Files.isRegularFile(service.getJMods(24).resolve("java.base.jmod")));
    }

    @Test
    void failsIfNoDistributionHasJmods() {
        runtimes.put("temurin", List.of(runtime("temurin", "jdk", 25)));
        runtimes.put("zulu", List.of());

        JavaProcessException e = assertThrows(JavaProcessException.class, () -> service.getJMods(25));
        assertEquals(1, e.getSuppressed().length);
        assertFalse(Files.exists(appFiles.getJModsDir().resolve("25")));
    }

    @Test
    void nothingIsDownloadedIfDownloadsAreDisabled() {
        download = false;
        runtimes.put("zulu", List.of(runtime("zulu", "jdk", 25)));
        withJMods.add("zulu-jdk-25");

        assertThrows(JavaProcessException.class, () -> service.getJMods(25));
        assertTrue(downloads.isEmpty());
    }

}
