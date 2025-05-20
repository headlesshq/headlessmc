package io.github.headlesshq.headlessmc.api.newapi;

import io.github.headlesshq.headlessmc.api.newapi.command.CommandLineManager;
import io.github.headlesshq.headlessmc.api.newapi.di.InjectorManager;

public interface Application {
    // threading
    MainThread getMainThread();

    InjectorManager getInjector();

    CommandLineManager getCommandLine();

    // commands
    // settings
    // Dependency Injection
    // ExitManager
    // logging

}
