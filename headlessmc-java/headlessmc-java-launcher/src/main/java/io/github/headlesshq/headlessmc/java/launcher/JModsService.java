package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.distribution.InstalledJavaRuntimeService;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds the jmods of a Java version.
 *
 * <p>Since JEP 493 (Java 24) some distributions, like temurin, do not ship their {@code jmods} directory anymore.
 * If the jmods are not part of an installation they are downloaded, once per Java major version,
 * into {@link AppFiles#getJModsDir()}{@code /<version>}.
 * No distribution provider offers jmods on their own, so a full JDK of one of the
 * {@link JavaConfig#jmodsDistributions()} is downloaded and only its {@code .jmod} files are kept.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JModsService {
    public static final String JMOD_EXTENSION = ".jmod";
    public static final String JAVA_BASE = "java.base" + JMOD_EXTENSION;
    public static final String PACKAGE_TYPE_JDK = "jdk";

    private final JavaDistributionProviderService distributionService;
    private final InstalledJavaRuntimeService installedRuntimeService;
    private final Holder<JavaConfig> config;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final CPU cpu;
    private final OS os;

    /**
     * @param java the Java installation to get the jmods for.
     * @return the jmods directory of the given installation, or the cached jmods of its version, without downloading.
     */
    public Optional<Path> findJMods(Java java) {
        Optional<Path> home = java.home().getUnchecked();
        if (home.isPresent()) {
            Path jmods = home.get().resolve("jmods");
            if (hasJMods(jmods)) {
                return Optional.of(jmods);
            }
        }

        return findCachedJMods(java.version());
    }

    /**
     * @param version the Java major version.
     * @return the directory containing the cached jmods for the given version, if they have been downloaded already.
     */
    public Optional<Path> findCachedJMods(int version) {
        Path dir = getCacheDir(version);
        return hasJMods(dir) ? Optional.of(dir) : Optional.empty();
    }

    /**
     * Returns the cached jmods for the given version, downloading them if necessary.
     *
     * @param version the Java major version.
     * @return a directory containing the jmods files of the given version.
     * @throws JavaProcessException if the jmods have not been cached and could not be downloaded.
     */
    public synchronized Path getJMods(int version) throws JavaProcessException {
        Optional<Path> cached = findCachedJMods(version);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (!config.get().download()) {
            throw new JavaProcessException("No jmods for Java " + version + " found and downloads are disabled");
        }

        Path dir = getCacheDir(version);
        download(version, dir);
        return dir;
    }

    public Path getCacheDir(int version) {
        return appFiles.getJModsDir().resolve(String.valueOf(version));
    }

    private void download(int version, Path dir) throws JavaProcessException {
        JavaProcessException exception = new JavaProcessException.InstallationException(
            "Failed to download jmods for Java " + version
        );

        JavaDistributionProvider provider = distributionService.getDefaultProvider();
        for (String distributionName : config.get().jmodsDistributions()) {
            try {
                Optional<JavaRuntime> runtime = findJdk(provider, distributionName, version);
                if (runtime.isEmpty()) {
                    log.debug("Distribution {} has no JDK for Java {}", distributionName, version);
                    continue;
                }

                download(provider, runtime.get(), dir);
                return;
            } catch (HeadlessMcException e) {
                log.debug("Failed to download jmods from {}", distributionName, e);
                exception.addSuppressed(e);
            }
        }

        throw exception;
    }

    private Optional<JavaRuntime> findJdk(JavaDistributionProvider provider, String distributionName, int version)
        throws HeadlessMcException {
        JavaDistribution distribution = provider.getDistributionByName(distributionName);
        List<JavaRuntime> runtimes = provider.getRuntimes(distribution, version, os, cpu);
        return runtimes.stream()
            .filter(runtime -> PACKAGE_TYPE_JDK.equalsIgnoreCase(runtime.packageType()))
            .findFirst();
    }

    private void download(JavaDistributionProvider provider, JavaRuntime runtime, Path dir) throws HeadlessMcException {
        log.info("Downloading jmods for Java {} from {}", runtime.version(), runtime.name());
        fileService.atomic(dir, jmodsDir -> {
            fileService.temp(jdkDir -> {
                Path installation = jdkDir.resolve("jdk");
                provider.download(runtime, installation);
                moveJMods(installation, jmodsDir);
            });

            if (!hasJMods(jmodsDir)) {
                throw new JavaProcessException.InstallationException(
                    runtime.name() + " (" + runtime.distribution() + ") does not contain jmods"
                );
            }

            // records where the jmods came from
            installedRuntimeService.write(jmodsDir, runtime);
        });
    }

    private void moveJMods(Path installation, Path target) throws FileException {
        // the jmods directory might be nested, e.g. on macOS: <jdk>/Contents/Home/jmods
        try (Stream<Path> files = Files.walk(installation)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Path parent = file.getParent();
                Path fileName = file.getFileName();
                if (parent != null
                    && fileName != null
                    && fileName.toString().endsWith(JMOD_EXTENSION)
                    && parent.getFileName() != null
                    && "jmods".equals(parent.getFileName().toString())
                    && Files.isRegularFile(file)) {
                    Files.move(file, target.resolve(fileName.toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new FileException("Failed to move jmods from " + installation + " to " + target, e);
        }
    }

    private static boolean hasJMods(Path dir) {
        return Files.isRegularFile(dir.resolve(JAVA_BASE));
    }

}
