package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JavaFinderTest {
    private record Downloaded(JavaRuntime runtime, Path path) {}

    @TempDir
    Path root;

    private final List<Java> javas = new ArrayList<>();
    private final List<JavaRuntime> runtimes = new ArrayList<>();
    private final List<Downloaded> downloads = new ArrayList<>();

    private boolean download = true;
    private int refreshes;

    /** Registered when {@link #refreshes} reaches 1, simulating a successful installation. */
    private Java installed;

    private final JavaService javaService = new JavaService() {
        @Override
        public Optional<Java> getJava(int version) {
            return javas.stream().filter(java -> java.version() == version).findFirst();
        }

        @Override
        public List<Java> getJavaVersions() {
            return javas;
        }

        @Override
        public List<JavaSource> getSources() {
            return List.of();
        }

        @Override
        public void refresh() {
            refreshes++;
            if (installed != null) {
                javas.add(installed);
            }
        }
    };

    private final JavaDistribution temurin = new JavaDistribution("foojay.io", "temurin", "temurin");

    private final JavaDistributionProvider provider = new JavaDistributionProvider() {
        @Override
        public String getName() {
            return "foojay.io";
        }

        @Override
        public List<JavaDistribution> getDistributions() {
            return List.of(temurin);
        }

        @Override
        public JavaDistribution getDistributionByName(String name) {
            return temurin;
        }

        @Override
        public List<JavaRuntime> getRuntimes(JavaDistribution distribution, int version, OS os, CPU cpu) {
            return runtimes;
        }

        @Override
        public List<JavaRuntime> getUpdates(JavaRuntime runtime) {
            return List.of();
        }

        @Override
        public void download(JavaRuntime runtime, Path path) {
            downloads.add(new Downloaded(runtime, path));
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
    private JavaFinder finder;

    @BeforeEach
    void setup() {
        appFiles = TestFiles.appFiles(root);
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
        };

        finder = new JavaFinder(providerService, javaService, config, appFiles, CPU.X64,
            new OS("linux", OS.Type.LINUX, "6.0"));
    }

    private Java java(int version) {
        Path home = root.resolve("java-" + version);
        return new Java("java-" + version, version, new SafePath(home), new SafePath(home.resolve("bin/java")), false, 0);
    }

    @Test
    void installedJavaIsReturned() {
        Java java = java(21);
        javas.add(java);

        assertEquals(java, finder.findJava(21));
        assertEquals(0, refreshes);
    }

    @Test
    void withoutDownloadMissingJavaThrows() {
        download = false;
        JavaProcessException e = assertThrows(JavaProcessException.class, () -> finder.findJava(21));
        assertTrue(e.getMessage().contains("Failed to find Java 21"));
    }

    @Test
    void missingRuntimeThrows() {
        JavaProcessException.InstallationException e = assertThrows(
            JavaProcessException.InstallationException.class, () -> finder.findJava(21)
        );
        assertTrue(e.getMessage().contains("Failed to find Java version 21"));
    }

    @Test
    void javaIsDownloadedAndFoundAfterRefresh() {
        runtimes.add(new JavaRuntime("foojay.io", "temurin", "id", "temurin-21", 21, null));
        installed = java(21);

        assertEquals(installed, finder.findJava(21));
        assertEquals(1, refreshes);
        assertEquals(appFiles.getJavaDir().resolve("temurin-21"), downloads.getFirst().path());
    }

    @Test
    void downloadThatDoesNotYieldJavaThrows() {
        runtimes.add(new JavaRuntime("foojay.io", "temurin", "id", "temurin-21", 21, null));

        JavaProcessException.InstallationException e = assertThrows(
            JavaProcessException.InstallationException.class, () -> finder.findJava(21)
        );
        assertTrue(e.getMessage().contains("even after installing"));
    }

}
