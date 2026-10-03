package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JavaCommandsTest {
    private record Downloaded(JavaRuntime runtime, Path path) {}

    private static final OS OS_LINUX = new OS("linux", OS.Type.LINUX, "6.0");

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();

    private final List<Downloaded> downloads = new ArrayList<>();
    private final List<JavaRuntime> runtimes = new ArrayList<>();
    private final List<Java> javas = new ArrayList<>();

    private int refreshes;

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
        }
    };

    private final JavaDistribution temurin = new JavaDistribution("foojay.io", "temurin", "temurin");
    private final JavaDistribution zulu = new JavaDistribution("foojay.io", "zulu", "zulu");

    private final JavaDistributionProvider foojay = new JavaDistributionProvider() {
        @Override
        public String getName() {
            return "foojay.io";
        }

        @Override
        public List<JavaDistribution> getDistributions() {
            return List.of(temurin, zulu);
        }

        @Override
        public JavaDistribution getDistributionByName(String name) {
            return getDistributions().stream()
                .filter(distribution -> distribution.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new HeadlessMcException("Unknown distribution " + name));
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
            return List.of(foojay);
        }

        @Override
        public Optional<JavaDistributionProvider> getProviderByName(String name) {
            return getProviders().stream().filter(provider -> provider.getName().equals(name)).findFirst();
        }

        @Override
        public JavaDistributionProvider getDefaultProvider() {
            return foojay;
        }
    };

    private AppFiles appFiles;
    private JavaCommand context;

    @BeforeEach
    void setup() {
        appFiles = TestFiles.appFiles(root);
        context = new JavaCommand(providerService);
    }

    private Java java(String name, int version, int source) {
        Path home = root.resolve(name);
        return new Java(name, version, new SafePath(home), new SafePath(home.resolve("bin/java")), false, source);
    }

    // --------------------------------------------------------- JavaCommand

    @Test
    void defaultProviderIsUsedWithoutOption() {
        assertSame(foojay, context.getService());
    }

    @Test
    void namedProviderIsResolved() {
        context.setProvider("foojay.io");
        assertSame(foojay, context.getService());
    }

    @Test
    void unknownProviderThrows() {
        context.setProvider("nope");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, context::getService);
        assertTrue(e.getMessage().contains("Failed to find java distribution provider nope"));
    }

    @Test
    void defaultDistributionIsTemurin() {
        assertEquals(temurin, context.getDistribution(foojay));
    }

    @Test
    void namedDistributionIsResolved() {
        context.setDistribution("zulu");
        assertEquals(zulu, context.getDistribution(foojay));
    }

    @Test
    void unknownDistributionThrows() {
        context.setDistribution("nope");
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> context.getDistribution(foojay)
        );
        assertTrue(e.getMessage().contains("Failed to find distribution nope"));
    }

    // ------------------------------------------------------ InstallCommand

    private InstallCommand install() {
        InstallCommand command = new InstallCommand(
            providerService, new DefaultFileService(new DefaultFileSystemProvider()), javaService, appFiles, console, OS_LINUX, CPU.X64
        );
        command.context = context;
        return command;
    }

    @Test
    void installWithoutRuntimesThrows() {
        InstallCommand command = install();
        command.setVersion(21);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("Failed to find a java 21 runtime"));
    }

    @Test
    void installDownloadsFirstRuntime() {
        JavaRuntime runtime = new JavaRuntime("foojay.io", "temurin", "id-21", "temurin-21", 21, null);
        runtimes.add(runtime);
        runtimes.add(new JavaRuntime("foojay.io", "temurin", "id-21b", "temurin-21b", 21, null));

        InstallCommand command = install();
        command.setVersion(21);
        command.run();

        assertEquals(1, downloads.size());
        assertEquals(runtime, downloads.getFirst().runtime());
        assertEquals(appFiles.getJavaDir().resolve("temurin-21"), downloads.getFirst().path());
        assertEquals(1, refreshes);
        assertTrue(console.output().contains("Installed java 21 (temurin)"));
    }

    @Test
    void installExistingRuntimeRequiresForce() throws IOException {
        runtimes.add(new JavaRuntime("foojay.io", "temurin", "id-21", "temurin-21", 21, null));
        Files.createDirectories(appFiles.getJavaDir().resolve("temurin-21"));

        InstallCommand command = install();
        command.setVersion(21);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("already exists"));

        command.setForce(true);
        command.run();
        assertEquals(1, downloads.size());
    }

    // --------------------------------------------------------- ListCommand

    private ListCommand list() {
        ListCommand command = new ListCommand(providerService, tables, javaService, console, OS_LINUX, CPU.X64);
        command.context = context;
        return command;
    }

    @Test
    void listShowsInstalledJavas() {
        javas.add(java("temurin-21", 21, JavaSource.SORT_HMC));

        list().run();

        assertEquals(1, refreshes);
        String output = console.output();
        assertTrue(output.contains("temurin-21"));
        assertTrue(output.contains("21"));
    }

    @Test
    void listProviders() {
        ListCommand command = list();
        command.setProviders(true);
        command.run();

        assertTrue(console.output().contains("foojay.io"));
        assertEquals(0, refreshes);
    }

    @Test
    void listRemoteDistributions() {
        ListCommand command = list();
        command.setRemote(true);
        command.run();

        String output = console.output();
        assertTrue(output.contains("temurin"));
        assertTrue(output.contains("zulu"));
    }

    @Test
    void listRemoteDistributionsOfSingleProvider() {
        context.setProvider("foojay.io");

        ListCommand command = list();
        command.setRemote(true);
        command.run();

        assertTrue(console.output().contains("temurin"));
    }

    @Test
    void listRemoteRuntimesForVersion() {
        runtimes.add(new JavaRuntime("foojay.io", "temurin", "id-21", "temurin-21", 21, null));

        ListCommand command = list();
        command.setRemote(true);
        command.setVersion(21);
        command.run();

        assertTrue(console.output().contains("temurin-21"));
        assertTrue(console.output().contains("id-21"));
    }

    @Test
    void listProvidersAndRemote() {
        ListCommand command = list();
        command.setProviders(true);
        command.setRemote(true);
        command.run();

        String output = console.output();
        assertTrue(output.contains("foojay.io"));
        assertTrue(output.contains("zulu"));
    }

    // ------------------------------------------------------- RemoveCommand

    private RemoveCommand remove() {
        return new RemoveCommand(javaService, new DefaultFileService(new DefaultFileSystemProvider()), console);
    }

    @Test
    void removeWithoutNameThrows() {
        assertThrows(IllegalArgumentException.class, remove()::run);
    }

    @Test
    void removeUnknownJavaThrows() {
        RemoveCommand command = remove();
        command.setName("nope");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("Failed to find java installation nope"));
    }

    @Test
    void removeDeletesInstallation() {
        Java java = java("temurin-21", 21, JavaSource.SORT_HMC);
        javas.add(java);
        createDir(java.home().getUnchecked().orElseThrow());

        RemoveCommand command = remove();
        command.setName("TEMURIN-21");
        command.run();

        assertFalse(Files.exists(java.home().getUnchecked().orElseThrow()));
        assertEquals(1, refreshes);
        assertTrue(console.output().contains("Removed java installation TEMURIN-21"));
        assertFalse(console.output().contains("Warning"));
    }

    @Test
    void removeForeignInstallationWarns() {
        Java java = java("system-17", 17, JavaSource.SORT_GENERAL);
        javas.add(java);
        createDir(java.home().getUnchecked().orElseThrow());

        RemoveCommand command = remove();
        command.setName("system-17");
        command.run();

        assertTrue(console.output().contains("was not installed by HeadlessMc"));
    }

    @Test
    void removeInstallationWithoutPathThrows() {
        javas.add(new Java("broken", 21, new SafePath(null), new SafePath(null), false, JavaSource.SORT_HMC));

        RemoveCommand command = remove();
        command.setName("broken");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("Failed to resolve path"));
    }

    // -------------------------------------------------------- completions

    @Test
    void installedJavaCompletions() {
        javas.add(java("temurin-21", 21, JavaSource.SORT_HMC));
        assertEquals(List.of("temurin-21"), toList(new InstalledJavaCompletions(javaService)));
    }

    @Test
    void javaProviderCompletions() {
        assertEquals(List.of("foojay.io"), toList(new JavaProviderCompletions(providerService)));
    }

    private List<String> toList(Iterable<String> iterable) {
        List<String> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    private void createDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
