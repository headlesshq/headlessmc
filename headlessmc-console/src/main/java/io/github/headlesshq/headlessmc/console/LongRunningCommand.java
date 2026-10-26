package io.github.headlesshq.headlessmc.console;

/**
 * Marker interface to mark commands that usually take a while,
 * e.g. because they launch the game or download files.
 * Before such a command is run from the command line, HeadlessMc may check for updates,
 * as the check is negligible compared to the runtime of the command.
 */
public interface LongRunningCommand {
}
