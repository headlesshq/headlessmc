package io.github.headlesshq.headlessmc.runtime;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMcImpl;
import io.github.headlesshq.headlessmc.api.classloading.ApiClassloadingHelper;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import io.github.headlesshq.headlessmc.api.config.Config;
import io.github.headlesshq.headlessmc.api.config.HmcProperties;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.jline.JLineCommandLineReader;
import io.github.headlesshq.headlessmc.jline.JLineProperties;
import io.github.headlesshq.headlessmc.logging.LoggingService;
import io.github.headlesshq.headlessmc.runtime.commands.RuntimeContext;

import java.util.logging.Level;

/**
 * Gives you a Framework to construct your own {@link HeadlessMc} instances.
 */
public class RuntimeInitializer {
    public void init(Config config) {
        LoggingService loggingService = new LoggingService();
        loggingService.setFileHandler(config.get(HmcProperties.FILE_HANDLER_ENABLED, false)); // usually no FileHandler at runtime
        loggingService.init();
        loggingService.setLevel(Level.INFO);

        HeadlessMc hmc = instance(config, loggingService);
        createCommandContext(hmc);
        readCommandLine(hmc);
    }

    protected HeadlessMc instance(Config config, LoggingService loggingService) {
        HeadlessMc hmc = new HeadlessMcImpl(() -> config, new CommandLineManager(), new ExitManager(), loggingService);
        HeadlessMc.setInstance(hmc);
        return hmc;
    }

    protected void createCommandContext(HeadlessMc hmc) {
        RuntimeContext context = new RuntimeContext(hmc);
        hmc.getCommandLine().setCommandContext(context);
        hmc.getCommandLine().setBaseContext(context);
    }

    protected void readCommandLine(HeadlessMc hmc) {
        if (ApiClassloadingHelper.installOnOtherInstances(hmc) == null) {
            if (hmc.getConfig().get(JLineProperties.ENABLED, true)) {
                // TODO: see hmc-specifics, fail early if JLineCommandLineReader fails?!
                hmc.getCommandLine().setCommandLineProvider(JLineCommandLineReader::new);
            }

            hmc.getCommandLine().readAsync(hmc);
        }
    }

}
