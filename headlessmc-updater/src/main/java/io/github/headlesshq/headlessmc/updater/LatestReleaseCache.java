package io.github.headlesshq.headlessmc.updater;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Caches the latest release of HeadlessMc fetched by the {@link UpdateService}.
 */
@ApplicationScoped
public class LatestReleaseCache {
    static final int CACHE_VERSION = 1;

    private final UpdateCheckCache<LatestRelease> cache;

    @Inject
    public LatestReleaseCache(UpdateCheckCacheFactory factory) {
        this.cache = factory.create(HeadlessMcUpdater.NAME, CACHE_VERSION, new TypeLiteral<UpdateCheck<LatestRelease>>() {});
    }

    /**
     * @return the latest cached release, or empty if nothing has been cached yet.
     */
    public Optional<UpdateCheck<LatestRelease>> get() {
        return cache.get();
    }

    /**
     * Caches a release that has just been fetched.
     *
     * @param latestRelease the latest release.
     */
    public void set(LatestRelease latestRelease) {
        cache.set(latestRelease);
    }

}
