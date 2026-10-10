package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.java.launcher.JavaFinder;
import io.github.headlesshq.headlessmc.patcher.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ClassSourceFactoryTest {
    private static final String TYPE = "io/github/headlesshq/headlessmc/patcher/nonclassloading/JarClassSource";

    /** A minimal {@link PatchContext} that only answers what the factory needs. */
    private record TestContext(PatchResult patchResult, int javaVersion) implements PatchContext {
        @Override
        public PatchResult getInitialPatchResult() {
            return patchResult;
        }

        @Override
        public PatchResult getCurrentPatchResult() {
            return patchResult;
        }

        @Override
        public OutputStream add(String library, Patcher patcher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream addAgent(String library, Patcher patcher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void patch(Path library, Patcher patcher, PatchAction action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addSystemProperty(String key, @Nullable String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Patcher> getPatchers() {
            return List.of();
        }

        @Override
        public int getJavaVersion() {
            return javaVersion;
        }

        @Override
        public <C extends HelperService> Stream<C> services(Class<C> type) {
            return Stream.empty();
        }

        @Override
        public PatchCache.Key getCacheKey() {
            throw new UnsupportedOperationException();
        }
    }

    @TempDir
    Path root;

    private final List<Java> javas = new ArrayList<>();

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
        }
    };

    private Java downloaded;

    private ClassSourceFactory factory;

    @BeforeEach
    void setup() {
        JavaFinder javaFinder = Mockito.mock(JavaFinder.class);
        Mockito.when(javaFinder.findJava(Mockito.anyInt())).thenAnswer(invocation -> {
            if (downloaded == null) {
                throw new PatchException("no java " + invocation.getArgument(0));
            }

            return downloaded;
        });

        factory = new ClassSourceFactory(javaService, javaFinder);
    }

    private static byte[] classBytes() {
        try (InputStream stream = ClassSourceFactoryTest.class.getClassLoader()
            .getResourceAsStream(TYPE + ".class")) {
            return java.util.Objects.requireNonNull(stream, "class file not found").readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path jar(Path file, String entry) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file);
                 JarOutputStream jar = new JarOutputStream(out)) {
                jar.putNextEntry(new JarEntry(entry));
                jar.write(classBytes());
                jar.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return file;
    }

    private Path zip(Path file, String entry) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file);
                 ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(classBytes());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return file;
    }

    private Java java(int version, Path home) {
        return new Java("java-" + version, version, new SafePath(home),
            new SafePath(home.resolve("bin/java")), false, 0);
    }

    private PatchContext context(Path... classpath) {
        return new TestContext(new PatchResult(new LinkedHashSet<>(List.of(classpath)), new LinkedHashSet<>()), 21);
    }

    @Test
    void jarClassSourcesReadClassesFromJars() throws IOException {
        Path jarFile = jar(root.resolve("lib.jar"), TYPE + ".class");

        try (ClassSource source = ClassSource.Provider.jar(jarFile, 21, JarClassSource::new).open()) {
            assertTrue(source.getClass(TYPE).isPresent());
            assertTrue(source.getClass("does/not/Exist").isEmpty());
        }
    }

    @Test
    void jmodClassSourcesReadClassesFromTheClassesFolder() throws IOException {
        Path jmod = zip(root.resolve("java.base.jmod"), "classes/" + TYPE + ".class");

        try (ClassSource source = ClassSource.Provider.zip(jmod, JModClassSource::new).open()) {
            assertTrue(source.getClass(TYPE).isPresent());
            assertTrue(source.getClass("does/not/Exist").isEmpty());
        }
    }

    @Test
    void missingArchivesAreReported() {
        assertThrows(RuntimeException.class,
            () -> ClassSource.Provider.jar(root.resolve("nope.jar"), 21, JarClassSource::new).open());
        assertThrows(RuntimeException.class,
            () -> ClassSource.Provider.zip(root.resolve("nope.zip"), JModClassSource::new).open());
    }

    @Test
    void jmodsOfTheMatchingJavaVersionAreUsed() {
        Path home = root.resolve("java21");
        zip(home.resolve("jmods").resolve("java.base.jmod"), "classes/" + TYPE + ".class");
        Files.exists(home);
        javas.add(java(21, home));

        List<ClassSource.Provider> providers = factory.create(context(jar(root.resolve("mc.jar"), "a.class")));

        // one provider for the classpath jar, one for the jmod
        assertEquals(2, providers.size());
    }

    @Test
    void anRtJarIsPreferredOverJmods() throws IOException {
        Path home = root.resolve("java8");
        jar(home.resolve("jre").resolve("lib").resolve("rt.jar"), TYPE + ".class");
        javas.add(java(21, home));

        List<ClassSource.Provider> providers = factory.jvmProviders(context());

        assertEquals(1, providers.size());
        try (ClassSource source = providers.getFirst().open()) {
            assertTrue(source.getClass(TYPE).isPresent());
        }
    }

    @Test
    void aJavaVersionIsDownloadedIfNoneIsInstalled() {
        Path home = root.resolve("downloaded");
        zip(home.resolve("jmods").resolve("java.base.jmod"), "classes/" + TYPE + ".class");
        downloaded = java(21, home);

        assertEquals(1, factory.jvmProviders(context()).size());
    }

    @Test
    void withoutJmodsOrAnRtJarTheFactoryFails() {
        javas.add(java(21, root.resolve("empty-java")));

        assertThrows(PatchException.class, () -> factory.jvmProviders(context()));
    }

    @Test
    void javaInstallationsWithoutAHomeAreRejected() {
        javas.add(new Java("broken", 21, new SafePath(null), new SafePath(null), false, 0));

        assertThrows(PatchException.class, () -> factory.jvmProviders(context()));
    }

    @Test
    void theStrategyIsAlwaysApplicable() {
        NonClassLoadingSuperClassStrategy strategy = new NonClassLoadingSuperClassStrategy(factory);

        assertTrue(strategy.isApplicable());
        assertEquals(
            io.github.headlesshq.headlessmc.patcher.asm.SuperClassStrategy.SORT_NON_CLASS_LOADING, strategy.sort()
        );
    }

}
