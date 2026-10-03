package io.github.headlesshq.headlessmc.progressbar;

public interface ProgressBarServiceManager {
    /**
     * Provides a {@link ProgressBar} for the given {@link ProgressBar.Configuration}.
     * Might be {@link ProgressBar#isDummy()} if the chosen {@link ProgressbarService}
     * only provides a dummy ProgressBar.
     *
     * @param configuration the configuration to configure the Progressbar with.
     * @return a Progressbar for the given configuration.
     */
    ProgressBar displayProgressBar(ProgressBar.Configuration configuration);

}
