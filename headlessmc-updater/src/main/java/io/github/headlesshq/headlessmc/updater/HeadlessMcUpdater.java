package io.github.headlesshq.headlessmc.updater;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * The {@link Updater} for HeadlessMc itself, checking GitHub for newer releases.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HeadlessMcUpdater implements Updater {
    /**
     * The name of this updater.
     */
    public static final String NAME = "headlessmc";

    private final UpdateService updateService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> checkForUpdates() {
        return updateService.checkForUpdate().map(Update::getMessageString);
    }

    @Override
    public Optional<String> checkForUpdatesIfStale() {
        return updateService.checkForUpdateIfStale().map(Update::getMessageString);
    }

}
