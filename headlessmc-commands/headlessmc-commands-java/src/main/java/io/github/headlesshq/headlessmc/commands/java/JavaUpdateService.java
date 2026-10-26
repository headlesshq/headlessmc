package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.distribution.InstalledJavaRuntime;
import io.github.headlesshq.headlessmc.distribution.InstalledJavaRuntimeService;
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

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Checks the Java installations inside the HeadlessMc java directory for updates and installs them.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaUpdateService {
    /**
     * Maps the {@code IMPLEMENTOR} of a {@code release} file to the distribution it belongs to.
     * Used for installations made before HeadlessMc recorded the {@link InstalledJavaRuntime}.
     */
    private static final Map<String, String> IMPLEMENTORS = Map.of(
        "Eclipse Adoptium", "temurin",
        "Azul Systems, Inc.", "zulu",
        "Amazon.com Inc.", "corretto",
        "BellSoft", "liberica",
        "Microsoft", "microsoft",
        "SAP SE", "sap_machine"
    );

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
                Optional<JavaRuntime> installed = getInstalledRuntime(java, installation);
                if (installed.isEmpty()) {
                    skipped.add(new Skipped(java, "unknown distribution"));
                    continue;
                }

                JavaDistributionProvider provider = getProvider(installed.get());
                provider.getUpdates(installed.get(), os, cpu)
                    .stream()
                    .findFirst()
                    .ifPresent(latest -> updates.add(new Update(java, installation, installed.get(), latest)));
            } catch (HeadlessMcException e) {
                log.error("Failed to check {} for updates", java.name(), e);
                skipped.add(new Skipped(java, String.valueOf(e.getMessage())));
            }
        }

        return new Result(updates, skipped);
    }

    /**
     * Installs the newer runtime of the given update and removes the outdated installation.
     *
     * @param update the update to install.
     * @return the directory the newer runtime has been installed to.
     * @throws HeadlessMcException if the installation fails.
     */
    public Path update(Update update) throws HeadlessMcException {
        JavaDistributionProvider provider = getProvider(update.latest());
        Path target = appFiles.getJavaDir().resolve(update.latest().name());
        if (Files.exists(target)) {
            log.info("{} is already installed at {}", update.latest().name(), target);
        } else {
            installedRuntimeService.install(provider, update.latest(), target);
        }

        if (!target.equals(update.installation())) {
            fileService.delete(update.installation());
        }

        javaService.refresh();
        return target;
    }

    private JavaDistributionProvider getProvider(JavaRuntime runtime) {
        return providerService.getProviderByName(runtime.provider())
            .orElseThrow(() -> new HeadlessMcException("Unknown java distribution provider " + runtime.provider()));
    }

    private Optional<JavaRuntime> getInstalledRuntime(Java java, Path installation) throws HeadlessMcException {
        Optional<JavaRuntime> recorded = installedRuntimeService.read(installation);
        if (recorded.isPresent()) {
            return recorded;
        }

        return java.home().getUnchecked().flatMap(home -> readReleaseFile(java, home));
    }

    private Optional<JavaRuntime> readReleaseFile(Java java, Path home) {
        Path releaseFile = home.resolve("release");
        if (!Files.isRegularFile(releaseFile)) {
            return Optional.empty();
        }

        Properties release = new Properties();
        try (Reader reader = Files.newBufferedReader(releaseFile, StandardCharsets.UTF_8)) {
            release.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to read {}", releaseFile, e);
            return Optional.empty();
        }

        String implementor = unquote(release.getProperty("IMPLEMENTOR"));
        String distribution = implementor == null ? null : IMPLEMENTORS.get(implementor);
        String javaVersion = unquote(release.getProperty("JAVA_RUNTIME_VERSION", release.getProperty("JAVA_VERSION")));
        if (distribution == null || javaVersion == null) {
            return Optional.empty();
        }

        boolean jdk = Files.exists(home.resolve("bin").resolve("javac"))
            || Files.exists(home.resolve("bin").resolve("javac.exe"));
        return Optional.of(new JavaRuntime(
            JavaDistributionProvider.DEFAULT,
            distribution,
            "",
            java.name(),
            java.version(),
            new JavaRuntime.MetaData(null, null, javaVersion, jdk ? "jdk" : "jre")
        ));
    }

    private static @Nullable String unquote(@Nullable String value) {
        if (value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    /**
     * @param updates the installations for which a newer runtime is available.
     * @param skipped the installations that could not be checked for updates.
     */
    public record Result(List<Update> updates, List<Skipped> skipped) {}

    /**
     * @param java         the installed Java.
     * @param installation the directory inside the HeadlessMc java directory containing the installation.
     * @param installed    the installed runtime.
     * @param latest       the newest runtime available.
     */
    public record Update(Java java, Path installation, JavaRuntime installed, JavaRuntime latest) {}

    /**
     * @param java   the installed Java.
     * @param reason why the installation could not be checked.
     */
    public record Skipped(Java java, String reason) {}

}
