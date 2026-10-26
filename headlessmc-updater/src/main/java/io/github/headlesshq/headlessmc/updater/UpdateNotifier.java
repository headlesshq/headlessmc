package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.console.Console;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Checks GitHub for a newer release of HeadlessMc in the background
 * and prints how to update to it, so that the shell does not have to wait for GitHub.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UpdateNotifier {
    private final UpdateService updateService;
    private final Console console;

    /**
     * Starts the check on a daemon thread.
     * Whether the check is enabled is decided by {@link UpdateService#checkForUpdate()}.
     */
    public void checkAsync() {
        Thread thread = new Thread(this::check, "HeadlessMc-UpdateCheck");
        thread.setDaemon(true);
        thread.start();
    }

    void check() {
        try {
            updateService.checkForUpdate()
                .map(update -> String.join(System.lineSeparator(), update.getMessage()))
                .ifPresent(console::write);
        } catch (Exception e) {
            log.debug("Failed to check for updates", e);
        }
    }

}
