package io.github.headlesshq.headlessmc.api.command;

/**
 * A type that provides a {@link Progressbar}.
 */
@FunctionalInterface
public interface ProgressBarProvider {
    /**
     * Provides a {@link Progressbar} for the given {@link Progressbar.Configuration}.
     * Might be {@link Progressbar#dummy()} if this provider does not support something.
     *
     * @param configuration the configuration to configure the Progressbar with.
     * @return a Progressbar for the given configuration.
     */
    Progressbar displayProgressBar(Progressbar.Configuration configuration);

    static ProgressBarProvider dummy() {
        return config -> Progressbar.dummy();
    }

}
