package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessManager;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileLaunchEvent;
import io.github.headlesshq.headlessmc.test.CommandTestService;
import io.github.headlesshq.headlessmc.test.TestConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LifecycleService {
    private final CommandTestService testService;
    private final Holder<LauncherConfig> config;
    private final Holder<TestConfig> testConfig;
    private final Console console;
    private final JavaProcessManager processManager;
    private final Event<ProfileLaunchEvent> launchEvent;

    /**
     * Fires the {@link ProfileLaunchEvent} for the given profile, e.g. to back it up.
     * Call this before {@link #wrap(ProcessLauncher, int) launching} the profile.
     *
     * @param profile    the profile that is about to be launched.
     * @param autoBackup {@code false} to skip automatic backups for this launch.
     */
    public void beforeLaunch(Profile profile, boolean autoBackup) {
        launchEvent.fire(new ProfileLaunchEvent(profile, autoBackup));
    }

    public ProcessLifecycle wrap(ProcessLauncher launcher, int retries) {
        ProcessLifecycle lifecycle = new ProcessLifecycle(
            launcher,
            testService,
            config.get(),
            testConfig.get(),
            console,
            launcher.getGameDir(),
            processManager
        );

        lifecycle.setRetries(retries);
        return lifecycle;
    }

}
