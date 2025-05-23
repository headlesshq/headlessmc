package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.command.CommandLineReader;
import io.github.headlesshq.headlessmc.api.settings.Config;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

public class JLineCommandLineReaderProvider implements Provider<CommandLineReader> {
    private final Provider<CommandLineReader> fallback;
    private final JLineSettings jLineSettings;
    private final Config config;

    @Inject
    public JLineCommandLineReaderProvider(@Named("fallback") Provider<CommandLineReader> fallback,
                                          JLineSettings jLineSettings,
                                          Config config) {
        this.fallback = fallback;
        this.jLineSettings = jLineSettings;
        this.config = config;
    }

    @Override
    public CommandLineReader get() {
        if (config.get(jLineSettings.enabled)) {
            return new JLineCommandLineReader(jLineSettings);
        }

        return fallback.get();
    }

}
