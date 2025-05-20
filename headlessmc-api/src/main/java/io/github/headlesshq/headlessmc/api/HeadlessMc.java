package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.classloading.Deencapsulator;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import io.github.headlesshq.headlessmc.api.config.HasConfig;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.logging.Logger;
import io.github.headlesshq.headlessmc.logging.LoggingService;

/**
 * Represents a HeadlessMc instance.
 * An instance handles commands, logging and configuration and exiting the process.
 */
public interface HeadlessMc extends LogsMessages, HasConfig {
    /**
     * Safe Wrapper for dev.xdark.deencapsulation.Deencapsulation,
     * which can be used to access classes beyond Java 9 module boundaries.
     *
     * @return a service that can be used to access classes beyond Java 9 module boundaries.
     */
    Deencapsulator getDeencapsulator();

    /**
     * Returns the command line belonging to this instance.
     * The command line manages commands and the terminal for this HeadlessMc instance.
     *
     * @return the command line instance managing commands and the terminal for this HeadlessMc instance.
     */
    CommandLineManager getCommandLine();

    /**
     * Instead of calling {@link System#exit(int)} it is advised to call this instead.
     *
     * @return the manager managing exiting this java process.
     */
    ExitManager getExitManager();

    /**
     * The LoggingService for this instance.
     * Configures {@link Logger}s.
     *
     * @return the logging service for this instance.
     */
    LoggingService getLoggingService();

    /**
     * Logs the given message in a human-readable format instead of logging it.
     * This method is meant to be used instead of a logger for communicating
     * console output to the user.
     *
     * @param message the message to log on the console.
     */
    @Override
    default void log(String message) {
        getCommandLine().getInAndOutProvider().getOut().get().println(message);
    }

}
