package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default ProgressbarService for tests, the real default
 * implementation is not on this module's classpath.
 */
@ApplicationScoped
public class TestProgressbarService implements ProgressbarService {
    @Override
    public String getName() {
        return "test-dummy";
    }

    @Override
    public ProgressBar displayProgressBar(ProgressBar.Configuration configuration) {
        return ProgressBar.dummy();
    }

    @Override
    public boolean isDummy() {
        return true;
    }

}
