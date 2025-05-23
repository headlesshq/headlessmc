package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.command.CommandLineReader;
import io.github.headlesshq.headlessmc.api.settings.Config;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provider for {@link JLineCommandLineReader}s.
 */
public class JLineCommandLineReaderProvider implements Provider<CommandLineReader> {
    private final JlineProgressbarProvider progressbarProvider;
    private final Provider<CommandLineReader> fallback;
    private final JLineSettings jLineSettings;
    private final Config config;

    @Inject
    public JLineCommandLineReaderProvider(JlineProgressbarProvider progressbarProvider,
                                          @Fallback Provider<CommandLineReader> fallback,
                                          JLineSettings jLineSettings,
                                          Config config) {
        this.progressbarProvider = progressbarProvider;
        this.fallback = fallback;
        this.jLineSettings = jLineSettings;
        this.config = config;
    }

    @Override
    public CommandLineReader get() {
        if (config.get(jLineSettings.getEnabled())) {
            return new JLineCommandLineReader(progressbarProvider, jLineSettings);
        }

        return fallback.get();
    }

    /**
     * Qualifier to configure the {@link JLineCommandLineReaderProvider#fallback} injected.
     */
    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Fallback { }

}
