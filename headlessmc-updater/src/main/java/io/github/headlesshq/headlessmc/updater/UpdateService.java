package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Checks GitHub for newer releases of HeadlessMc.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UpdateService {
    /**
     * The name of the jar attached to every release.
     */
    public static final String JAR_ASSET = "headlessmc.jar";

    private static final String NATIVE_ASSET_PREFIX = "headlessmc-launcher-";
    private static final Map<String, String> NATIVE_OS = Map.of(
        OS.Type.WINDOWS.name(), "windows",
        OS.Type.LINUX.name(), "linux",
        OS.Type.MACOS.name(), "macos"
    );
    private static final Map<String, String> NATIVE_ARCH = Map.of(
        CPU.X64.architecture(), "x64",
        CPU.AARCH64.architecture(), "arm64"
    );

    private final Holder<UpdaterConfig> config;
    private final OSService osService;

    /**
     * Checks GitHub for a newer release of HeadlessMc.
     * The latest release on GitHub is considered newer if its tag differs from the running version.
     * This check never fails, if GitHub cannot be reached it is just logged.
     *
     * @return the latest release, or empty if it is the running version, the check is disabled or failed.
     */
    public Optional<Update> checkForUpdate() {
        UpdaterConfig updaterConfig = config.get();
        if (!updaterConfig.enabled()) {
            return Optional.empty();
        }

        String[] repository = updaterConfig.repository().split("/");
        if (repository.length != 2) {
            log.warn("Invalid repository for the updater, expected owner/repo, but got {}", updaterConfig.repository());
            return Optional.empty();
        }

        try {
            GitHubRelease latest = createApi(updaterConfig).getLatestRelease(repository[0], repository[1]);
            return isUpdate(HeadlessMc.VERSION, latest) ? Optional.of(toUpdate(latest)) : Optional.empty();
        } catch (ApiException | ProcessingException e) {
            log.debug("Failed to check for updates", e);
            return Optional.empty();
        }
    }

    /**
     * Checks if the latest release is not the running version.
     *
     * @param currentVersion the version of the running HeadlessMc.
     * @param latest         the latest release.
     * @return {@code true} if the tag of the latest release differs from the running version.
     */
    @VisibleForTesting
    boolean isUpdate(String currentVersion, GitHubRelease latest) {
        return latest.tagName() != null && !latest.tagName().trim().equalsIgnoreCase(currentVersion.trim());
    }

    /**
     * Determines the name of the release asset that replaces the given installation.
     *
     * @param type   the type of the installation.
     * @param osType the type of the operating system.
     * @param cpu    the CPU architecture.
     * @return the name of the asset, or {@code null} if no asset is published for this installation.
     */
    @VisibleForTesting
    @Nullable String getAssetName(Installation.Type type, OS.Type osType, CPU cpu) {
        return switch (type) {
            case JAR -> JAR_ASSET;
            case NATIVE -> {
                String os = NATIVE_OS.get(osType.name());
                String arch = NATIVE_ARCH.get(cpu.architecture());
                yield os == null || arch == null
                    ? null
                    : NATIVE_ASSET_PREFIX + os + "-" + arch + (OS.Type.WINDOWS.equals(osType) ? ".exe" : "");
            }
            case UNKNOWN -> null;
        };
    }

    private Update toUpdate(GitHubRelease release) {
        Installation installation = Installation.detect();
        OS.Type osType = osService.getOS().type();
        String assetName = getAssetName(installation.type(), osType, osService.getCPU());
        GitHubRelease.Asset asset = assetName == null ? null : release.getAsset(assetName).orElse(null);
        return new Update(HeadlessMc.VERSION, release, installation, asset, OS.Type.WINDOWS.equals(osType));
    }

    private GitHubAPI createApi(UpdaterConfig updaterConfig) {
        return RestClientBuilder.newBuilder()
            .baseUri(URI.create(GitHubAPI.API_URL))
            .connectTimeout(updaterConfig.timeoutMs(), TimeUnit.MILLISECONDS)
            .readTimeout(updaterConfig.timeoutMs(), TimeUnit.MILLISECONDS)
            .build(GitHubAPI.class);
    }

}
