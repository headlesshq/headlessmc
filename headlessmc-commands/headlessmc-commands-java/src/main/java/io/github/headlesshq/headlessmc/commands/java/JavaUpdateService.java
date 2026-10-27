package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.distribution.InstalledJavaRuntime;
import io.github.headlesshq.headlessmc.distribution.InstalledJavaRuntimeService;
import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProviderService;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Checks the Java installations inside the HeadlessMc java directory for updates and installs them.
 * Installations without a recorded {@link InstalledJavaRuntime} are always outdated,
 * they get replaced with the latest runtime of the default distribution for their major version.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaUpdateService {
    private final JavaDistributionProviderService providerService;
    private final InstalledJavaRuntimeService installedRuntimeService;
    private final JavaService javaService;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final OS os;
    private final CPU cpu;

    /**
     * Checks every Java installation in the HeadlessMc java directory for updates.
     *
     * @return the available updates and the installations that could not be checked.
     */
    public Result check() {
        List<Update> updates = new ArrayList<>();
        List<Skipped> skipped = new ArrayList<>();
        for (Java java : javaService.getJavaVersions()) {
            if (java.source() != JavaSource.SORT_HMC) {
                continue;
            }

            try {
                Path installation = appFiles.getJavaDir().resolve(java.name());
                Optional<JavaRuntime> installed = installedRuntimeService.read(installation);
                Optional<JavaRuntime> latest = installed.isPresent()
                    ? getProvider(installed.get()).getUpdate(installed.get(), os, cpu)
                    : Optional.of(getLatestDefaultRuntime(java.version()));
                latest.ifPresent(runtime -> updates.add(new Update(java, installation, installed.orElse(null), runtime)));
            } catch (HeadlessMcException e) {
                log.warn("Failed to check {} for updates: {}", java.name(), e.getMessage());
                log.debug("Failed to check {} for updates", java.name(), e);
                skipped.add(new Skipped(java, String.valueOf(e.getMessage())));
            }
        }

        return new Result(updates, skipped);
    }

    /**
     * Installs the newer runtime of the given update and removes the outdated installation,
     * but only after the newer runtime has been verified to be installed.
     *
     * @param update the update to install.
     * @return the directory the newer runtime has been installed to.
     * @throws HeadlessMcException if the installation fails.
     */
    public Path update(Update update) throws HeadlessMcException {
        JavaRuntime latest = update.latest();
        Path target = appFiles.getJavaDir().resolve(latest.name());
        if (isInstalled(latest.name())) {
            log.info("{} is already installed at {}", latest.name(), target);
            if (installedRuntimeService.read(target).isEmpty()) {
                installedRuntimeService.write(target, latest);
            }
        } else {
            installedRuntimeService.install(getProvider(latest), latest, target);
            javaService.refresh();
            if (!isInstalled(latest.name())) {
                throw new HeadlessMcException("Failed to find a java installation in " + target + " after installing it");
            }
        }

        if (!target.equals(update.installation())) {
            fileService.delete(update.installation());
            javaService.refresh();
        }

        return target;
    }

    /**
     * @param name the name of a directory inside the HeadlessMc java directory.
     * @return {@code true} if a Java installation has been found in the directory.
     */
    public boolean isInstalled(String name) {
        return javaService.getJavaVersions()
            .stream()
            .anyMatch(java -> java.source() == JavaSource.SORT_HMC && java.name().equals(name));
    }

    private JavaRuntime getLatestDefaultRuntime(int version) throws HeadlessMcException {
        JavaDistributionProvider provider = providerService.getDefaultProvider();
        JavaDistribution distribution = provider.getDistributionByName(JavaDistribution.DEFAULT);
        return provider.getRuntimes(distribution, version, os, cpu)
            .stream()
            .findFirst()
            .orElseThrow(() -> new HeadlessMcException(
                "Failed to find a java %s runtime for distribution %s".formatted(version, distribution.name())
            ));
    }

    private JavaDistributionProvider getProvider(JavaRuntime runtime) {
        return providerService.getProviderByName(runtime.provider())
            .orElseThrow(() -> new HeadlessMcException("Unknown java distribution provider " + runtime.provider()));
    }

    /**
     * @param updates the installations for which a newer runtime is available.
     * @param skipped the installations that could not be checked for updates.
     */
    public record Result(List<Update> updates, List<Skipped> skipped) {}

    /**
     * @param java         the installed Java.
     * @param installation the directory inside the HeadlessMc java directory containing the installation.
     * @param installed    the installed runtime, or {@code null} if it has not been recorded.
     * @param latest       the latest stable runtime available.
     */
    public record Update(Java java, Path installation, @Nullable JavaRuntime installed, JavaRuntime latest) {
        /**
         * @return the full version of the installed runtime, or its major version if it has not been recorded.
         */
        public String installedVersion() {
            return installed == null ? String.valueOf(java.version()) : versionOf(installed);
        }

        /**
         * @return the full version of the latest runtime.
         */
        public String latestVersion() {
            return versionOf(latest);
        }

        private static String versionOf(JavaRuntime runtime) {
            return Objects.requireNonNullElse(runtime.javaVersion(), String.valueOf(runtime.version()));
        }
    }

    /**
     * @param java   the installed Java.
     * @param reason why the installation could not be checked.
     */
    public record Skipped(Java java, String reason) {}

}
