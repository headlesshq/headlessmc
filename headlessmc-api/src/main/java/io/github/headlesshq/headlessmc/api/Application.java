package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.api.command.CommandLineManager;
import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.logging.Out;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;

public interface Application extends Out {
    SettingGroup getSettings();

    Injector getInjector();

    CommandLineManager getCommandLine();

    ExitManager getExitManager();

    Config getConfig();

    Object getLock();

    @Override
    default void log(String message) {
        getCommandLine().getStdIO().log(message);
    }

}
