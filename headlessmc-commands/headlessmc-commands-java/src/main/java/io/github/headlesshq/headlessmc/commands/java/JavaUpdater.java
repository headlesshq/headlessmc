package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import io.github.headlesshq.headlessmc.updater.UpdateCheck;
import io.github.headlesshq.headlessmc.updater.UpdateCheckCache;
import io.github.headlesshq.headlessmc.updater.UpdateCheckCacheFactory;
import io.github.headlesshq.headlessmc.updater.Updater;
import io.github.headlesshq.headlessmc.updater.UpdaterConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The {@link Updater} for the Java installations installed by HeadlessMc,
 * suggesting the {@link UpdateCommand} if any of them can be updated.
 */
@ApplicationScoped
public class JavaUpdater implements Updater {
    /**
     * The name of this updater.
     */
    public static final String NAME = "java";

    static final int CACHE_VERSION = 1;

    private final JavaUpdateService updateService;
    private final Holder<JavaConfig> javaConfig;
    private final Holder<UpdaterConfig> updaterConfig;
    private final UpdateCheckCache<JavaUpdates> cache;
    private final AppFiles appFiles;

    @Inject
    public JavaUpdater(
        JavaUpdateService updateService,
        Holder<JavaConfig> javaConfig,
        Holder<UpdaterConfig> updaterConfig,
        UpdateCheckCacheFactory cacheFactory,
        AppFiles appFiles
    ) {
        this.updateService = updateService;
        this.javaConfig = javaConfig;
        this.updaterConfig = updaterConfig;
        this.cache = cacheFactory.create(NAME, CACHE_VERSION, new TypeLiteral<UpdateCheck<JavaUpdates>>() {});
        this.appFiles = appFiles;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabled() {
        return javaConfig.get().checkForUpdates();
    }

    @Override
    public Optional<String> checkForUpdates() {
        JavaUpdates updates = new JavaUpdates(updateService.check().updates().stream()
            .map(update -> new JavaUpdates.Entry(String.valueOf(update.installation().getFileName()), update.latest().name()))
            .toList());
        cache.set(updates);
        return getMessage(updates.updates().size());
    }

    /**
     * Uses the cached updates if they are not stale.
     * Cached updates that have been installed since, e.g. with {@code java update}, are not reported.
     */
    @Override
    public Optional<String> checkForUpdatesIfStale() {
        Optional<UpdateCheck<JavaUpdates>> cached = cache.get();
        Duration interval = Duration.ofHours(updaterConfig.get().intervalHours());
        if (cached.isEmpty() || cached.get().isStale(Instant.now(), interval)) {
            return checkForUpdates();
        }

        List<JavaUpdates.Entry> pending = cached.get().value().updates().stream()
            .filter(this::isPending)
            .toList();
        return getMessage(pending.size());
    }

    private boolean isPending(JavaUpdates.Entry entry) {
        Path javaDir = appFiles.getJavaDir();
        return Files.exists(javaDir.resolve(entry.installation())) && !Files.exists(javaDir.resolve(entry.latest()));
    }

    private Optional<String> getMessage(int updates) {
        if (updates <= 0) {
            return Optional.empty();
        }

        return Optional.of(updates == 1
            ? "1 java installation can be updated, run 'java update' to update it."
            : updates + " java installations can be updated, run 'java update' to update them.");
    }

}
