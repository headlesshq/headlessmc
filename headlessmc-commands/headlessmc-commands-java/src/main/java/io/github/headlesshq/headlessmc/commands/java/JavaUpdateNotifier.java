package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Checks the Java installations installed by HeadlessMc for updates in the background
 * and suggests the {@link UpdateCommand} if any can be updated.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaUpdateNotifier {
    private final JavaUpdateService updateService;
    private final Holder<JavaConfig> config;
    private final Console console;

    /**
     * Starts the check on a daemon thread, unless disabled by {@link JavaConfig#checkForUpdates()}.
     */
    public void checkAsync() {
        if (!config.get().checkForUpdates()) {
            return;
        }

        Thread thread = new Thread(this::check, "HeadlessMc-JavaUpdateCheck");
        thread.setDaemon(true);
        thread.start();
    }

    void check() {
        try {
            int updates = updateService.check().updates().size();
            if (updates > 0) {
                console.write(updates == 1
                    ? "1 java installation can be updated, run 'java update' to update it."
                    : updates + " java installations can be updated, run 'java update' to update them.");
            }
        } catch (Exception e) {
            log.debug("Failed to check java installations for updates", e);
        }
    }

}
