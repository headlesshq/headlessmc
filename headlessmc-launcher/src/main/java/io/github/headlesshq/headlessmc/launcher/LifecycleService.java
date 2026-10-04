package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.test.CommandTestService;
import io.github.headlesshq.headlessmc.test.TestConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LifecycleService {
    private final CommandTestService testService;
    private final Holder<LauncherConfig> config;
    private final Holder<TestConfig> testConfig;
    private final Console console;

    public ProcessLifecycle wrap(ProcessLauncher launcher, int retries) {
        ProcessLifecycle lifecycle = new ProcessLifecycle(
            launcher,
            testService,
            config.get(),
            testConfig.get(),
            console,
            launcher.getGameDir()
        );

        lifecycle.setRetries(retries);
        return lifecycle;
    }

}
