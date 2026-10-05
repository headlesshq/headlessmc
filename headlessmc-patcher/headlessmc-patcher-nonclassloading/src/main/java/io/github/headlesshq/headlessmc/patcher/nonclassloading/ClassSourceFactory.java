package io.github.headlesshq.headlessmc.patcher.nonclassloading;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
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
    private final JavaService javaService;
    private final JavaFinder javaFinder;

    public List<ClassSource.Provider> create(PatchContext context) {
        List<ClassSource.Provider> providers = new ArrayList<>();
        context.getCurrentClasspath().files().stream()
            .map(file -> ClassSource.Provider.jar(file, context.getJavaVersion(), JarClassSource::new))
            .forEach(providers::add);
        providers.addAll(jvmProviders(context));
        return providers;
    }

    public List<ClassSource.Provider> jvmProviders(PatchContext context) {
        PatchException exception = new PatchException("Failed to find a Java version with jmods or an rt.jar");
        boolean foundJava = false;
        for (Java java : javaService.getJavaVersions()) {
            if (java.version() == context.getJavaVersion()) {
                foundJava = true;
                try {
                    Optional<List<ClassSource.Provider>> classSource = scan(java);
                    if (classSource.isPresent()) {
                        return classSource.get();
                    }
                } catch (PatchException e) {
                    exception.addSuppressed(e);
                }
            }
        }

        if (!foundJava) {
            Java java = javaFinder.findJava(context.getJavaVersion());
            Optional<List<ClassSource.Provider>> classSource = scan(java);
            if (classSource.isPresent()) {
                return classSource.get();
            }
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

        // TODO: API to download jmods for a distribution if not available
        // e.g. https://api.adoptium.net/v3/binary/latest/24/ga/windows/x64/jmods/hotspot/normal/eclipse
        // check if instead of temurin we should move to a distribution that still has jmods files
        Path jmods = javaHome.resolve("jmods");
        if (Files.exists(jmods) && Files.isDirectory(jmods)) {
            try (Stream<Path> jmodFiles = Files.list(jmods)) {
                return Optional.of(
                    jmodFiles.filter(file -> file.toString().endsWith(".jmod"))
                        .map(jmodFile -> ClassSource.Provider.zip(jmodFile, JModClassSource::new))
                        .toList()
                );
            } catch (IOException e) {
                throw new HeadlessMcIOException(e);
            }
        }

        return Optional.empty();
    }

}
