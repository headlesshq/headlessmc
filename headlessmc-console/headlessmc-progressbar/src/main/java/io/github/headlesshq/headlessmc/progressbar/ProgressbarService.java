package io.github.headlesshq.headlessmc.progressbar;

public interface ProgressbarService {
    String getName();

    /**
     * Provides a {@link ProgressBar} for the given {@link ProgressBar.Configuration}.
     * Might be {@link ProgressBar#isDummy()} if this provider does not support something.
     *
     * @param configuration the configuration to configure the Progressbar with.
     * @return a Progressbar for the given configuration.
     */
    ProgressBar displayProgressBar(ProgressBar.Configuration configuration);

    default boolean isDummy() {
        return false;
    }

}
