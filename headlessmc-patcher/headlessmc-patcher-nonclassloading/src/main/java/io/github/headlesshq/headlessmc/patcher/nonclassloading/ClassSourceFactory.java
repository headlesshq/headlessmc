package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.launcher.JModsService;
import io.github.headlesshq.headlessmc.java.launcher.JavaFinder;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Produces a {@link ClassSource} for the java version specified
 * by {@link PatchContext#getJavaVersion()}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ClassSourceFactory {
    /**
     * Since Java 9 the Java runtime is split into modules and there is no rt.jar anymore.
     */
    private static final int LAST_VERSION_WITH_RT_JAR = 8;

    private final JavaService javaService;
    private final JavaFinder javaFinder;
    private final JModsService jModsService;

    public List<ClassSource.Provider> create(PatchContext context) {
        List<ClassSource.Provider> providers = new ArrayList<>();
        context.getCurrentPatchResult().files().stream()
            .map(file -> ClassSource.Provider.jar(file, context.getJavaVersion(), JarClassSource::new))
            .forEach(providers::add);
        providers.addAll(jvmProviders(context));
        return providers;
    }

    public List<ClassSource.Provider> jvmProviders(PatchContext context) {
        int version = context.getJavaVersion();
        PatchException exception = new PatchException("Failed to find a Java " + version + " with jmods or an rt.jar");
        for (Java java : javaService.getJavaVersions()) {
            if (java.version() == version) {
                try {
                    Optional<List<ClassSource.Provider>> classSource = scan(java);
                    if (classSource.isPresent()) {
                        return classSource.get();
                    }
                } catch (HeadlessMcException e) {
                    exception.addSuppressed(e);
                }
            }
        }

        try {
            if (version > LAST_VERSION_WITH_RT_JAR) {
                // no need to download an entire Java runtime, we only need the jmods
                return jmods(jModsService.getJMods(version));
            }

            Java java = javaFinder.findJava(version);
            Optional<List<ClassSource.Provider>> classSource = scan(java);
            if (classSource.isPresent()) {
                return classSource.get();
            }
        } catch (PatchException e) {
            e.addSuppressed(exception);
            throw e;
        } catch (HeadlessMcException e) {
            exception.addSuppressed(e);
        }

        throw exception;
    }

    private Optional<List<ClassSource.Provider>> scan(Java java) {
        Path javaHome = java.home().getUnchecked()
            .orElseThrow(() -> new PatchException("Failed to get JAVA_HOME of java " + java));

        // one problem is macOS, there might not be a rt.jar
        // https://stackoverflow.com/a/2776410
        Path rtJar = javaHome.resolve("jre", "lib", "rt.jar");
        if (Files.exists(rtJar)) {
            return Optional.of(List.of(ClassSource.Provider.jar(rtJar, java.version(), JarClassSource::new)));
        }

        return jModsService.findJMods(java).map(this::jmods);
    }

    private List<ClassSource.Provider> jmods(Path jmods) {
        try (Stream<Path> jmodFiles = Files.list(jmods)) {
            return jmodFiles.filter(file -> file.toString().endsWith(JModsService.JMOD_EXTENSION))
                .map(jmodFile -> ClassSource.Provider.zip(jmodFile, JModClassSource::new))
                .toList();
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

}
