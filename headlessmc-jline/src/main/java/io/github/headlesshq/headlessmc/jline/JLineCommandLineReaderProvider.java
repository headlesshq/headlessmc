package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.command.CommandLineReader;
import io.github.headlesshq.headlessmc.api.command.DefaultCommandLineReaderProvider;
import io.github.headlesshq.headlessmc.api.logging.StdIO;
import io.github.headlesshq.headlessmc.api.settings.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;

/**
 * Provider for {@link JLineCommandLineReader}s.
 */
@ApplicationScoped
public class JLineCommandLineReaderProvider extends DefaultCommandLineReaderProvider {
    private final JLineSettings jLineSettings;
    private final Config config;

    @Inject
    public JLineCommandLineReaderProvider(@CommandLineReader.Implementation Instance<CommandLineReader> readers,
                                          StdIO stdIO,
                                          JLineSettings jLineSettings,
                                          Config config) {
        super(readers, stdIO);
        this.jLineSettings = jLineSettings;
        this.config = config;
    }

    @Override
    @Produces
    @Specializes
    @ApplicationScoped
    public CommandLineReader get() {
        if (config.get(jLineSettings.getEnabled())) {
            return getReaders().select(JLineCommandLineReader.class).get();
        }

        return super.get();
    }

}
