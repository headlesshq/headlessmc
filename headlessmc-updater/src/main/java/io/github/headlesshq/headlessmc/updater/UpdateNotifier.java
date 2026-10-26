package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Runs all {@link Updater}s and tells the user about the updates they found.
 */
@Slf4j
@ApplicationScoped
public class UpdateNotifier {
    private final Instance<Updater> updaters;
    private final Holder<UpdaterConfig> config;
    private final Console console;

    @Inject
    public UpdateNotifier(@Any Instance<Updater> updaters, Holder<UpdaterConfig> config, Console console) {
        this.updaters = updaters;
        this.config = config;
        this.console = console;
    }

    /**
     * Runs every updater on its own daemon thread, requesting remote sources,
     * so that the shell does not have to wait for them.
     */
    public void checkAsync() {
        for (Updater updater : getUpdaters()) {
            Thread thread = new Thread(() -> check(updater, Updater::checkForUpdates), "HeadlessMc-Updater-" + updater.getName());
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Runs every updater before a {@link LongRunningCommand} is run.
     * Remote sources are only requested if the cached results are stale, see {@link Updater#checkForUpdatesIfStale()}.
     *
     * @param command the command that is about to run, may be {@code null} if it could not be determined.
     */
    public void checkBefore(@Nullable Object command) {
        if (command instanceof LongRunningCommand) {
            for (Updater updater : getUpdaters()) {
                check(updater, Updater::checkForUpdatesIfStale);
            }
        }
    }

    List<Updater> getUpdaters() {
        if (!config.get().enabled()) {
            return List.of();
        }

        return updaters.stream()
            .filter(Updater::isEnabled)
            .sorted(Comparator.comparing(Updater::getName))
            .toList();
    }

    private void check(Updater updater, Function<Updater, Optional<String>> check) {
        try {
            check.apply(updater).ifPresent(console::write);
        } catch (Exception e) {
            log.debug("Updater {} failed to check for updates", updater.getName(), e);
        }
    }

}
