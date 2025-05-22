package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.api.command.CommandLineManager;
import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;

public interface Application {
    SettingGroup getSettings();

    Injector getInjector();

    CommandLineManager getCommandLine();

    ExitManager getExitManager();

    Config getConfig();

    Object getLock();

    default void message(String message) {
        getCommandLine().getStdIO().getOut().get().println(message);
    }

}
