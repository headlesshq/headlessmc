package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Checks GitHub for newer releases of HeadlessMc.
 */
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
    private final GitHubReleaseClient client;
    private final LatestReleaseCache cache;
    private final OSService osService;

    /**
     * Checks GitHub for a newer release of HeadlessMc and caches the latest release.
     * The latest release on GitHub is considered newer if its tag differs from the running version.
     * This check never fails, if GitHub cannot be reached the cached release is used.
     *
     * @return the newer release, or empty if the running version is the latest, the check is disabled or failed.
     */
    public Optional<Update> checkForUpdate() {
        UpdaterConfig updaterConfig = config.get();
        if (!updaterConfig.enabled()) {
            return Optional.empty();
        }

        Optional<GitHubRelease> latest = client.getLatestRelease(updaterConfig);
        latest.ifPresent(release -> cache.set(new LatestRelease(updaterConfig.repository(), release)));
        return latest.or(() -> getCache(updaterConfig).map(check -> check.value().release())).flatMap(this::toUpdate);
    }

    /**
     * Like {@link #checkForUpdate()}, but only requests GitHub if the cached release is older than
     * {@link UpdaterConfig#intervalHours()}. Otherwise, the cached release is used,
     * so a user who has not updated yet is still notified about the update.
     *
     * @return the newer release, or empty if the running version is the latest, the check is disabled or failed.
     */
    public Optional<Update> checkForUpdateIfStale() {
        UpdaterConfig updaterConfig = config.get();
        if (!updaterConfig.enabled()) {
            return Optional.empty();
        }

        Optional<UpdateCheck<LatestRelease>> cached = getCache(updaterConfig);
        if (cached.isEmpty() || cached.get().isStale(Instant.now(), Duration.ofHours(updaterConfig.intervalHours()))) {
            return checkForUpdate();
        }

        return cached.flatMap(check -> toUpdate(check.value().release()));
    }

    private Optional<UpdateCheck<LatestRelease>> getCache(UpdaterConfig updaterConfig) {
        return cache.get().filter(check -> updaterConfig.repository().equals(check.value().repository()));
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

    private Optional<Update> toUpdate(GitHubRelease release) {
        if (!isUpdate(HeadlessMc.VERSION, release)) {
            return Optional.empty();
        }

        Installation installation = Installation.detect();
        OS.Type osType = osService.getOS().type();
        String assetName = getAssetName(installation.type(), osType, osService.getCPU());
        GitHubRelease.Asset asset = assetName == null ? null : release.getAsset(assetName).orElse(null);
        return Optional.of(new Update(HeadlessMc.VERSION, release, installation, asset, OS.Type.WINDOWS.equals(osType)));
    }

}
