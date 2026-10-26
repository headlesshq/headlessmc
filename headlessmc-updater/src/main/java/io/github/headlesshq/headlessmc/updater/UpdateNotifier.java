package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Tells the user about newer releases of HeadlessMc and how to update to them.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UpdateNotifier {
    private final UpdateService updateService;
    private final Console console;

    /**
     * Checks GitHub on a daemon thread, so that the shell does not have to wait for GitHub.
     * Whether the check is enabled is decided by the {@link UpdateService}.
     */
    public void checkAsync() {
        Thread thread = new Thread(() -> check(updateService::checkForUpdate), "HeadlessMc-UpdateCheck");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Notifies about a newer release before a {@link LongRunningCommand} is run.
     * GitHub is only requested if the cached release is stale,
     * see {@link UpdateService#checkForUpdateIfStale()}.
     *
     * @param command the command that is about to run, may be {@code null} if it could not be determined.
     */
    public void checkBefore(@Nullable Object command) {
        if (command instanceof LongRunningCommand) {
            check(updateService::checkForUpdateIfStale);
        }
    }

    void check(Supplier<Optional<Update>> check) {
        try {
            check.get()
                .map(update -> String.join(System.lineSeparator(), update.getMessage()))
                .ifPresent(console::write);
        } catch (Exception e) {
            log.debug("Failed to check for updates", e);
        }
    }

}
