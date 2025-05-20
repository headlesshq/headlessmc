package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import lombok.val;
import io.github.headlesshq.headlessmc.api.command.QuickExitCli;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineReader;

/**
 * @see QuickExitCli
 */
@CustomLog
@UtilityClass
public class QuickExitCliHandler {
    /**
     * Checks if the args contain a "--command". If they do the rest of the args
     * will get collected into a command. If the args contain no "--command",
     * the command is empty or "cli" {@code false} will be returned. Otherwise
     * {@code true} will be returned, the given {@link Launcher}s command
     * context will be run on the given command and {@link
     * CommandLineManager#setQuickExitCli(boolean)} will be set to
     * {@code true}. If a command needs more input it can set
     * {@link CommandLineManager#setWaitingForInput(boolean)} to {@code true} which will
     * cause this method call the given {@link CommandLineReader}. An exception are the
     * args "--version", "-version" or "version". These will also return
     * {@code true} and print {@link Launcher#VERSION}.
     *
     * @param launcher the launcher.
     * @param args     the arguments to check.
     * @return {@code true} if the launcher shouldn't listen to more commands.
     */
    public static boolean checkQuickExit(Launcher launcher, String... args) {
        val cmd = collectArgs(launcher, args);
        if (cmd != null) {
            if (cmd.isEmpty() || "cli".equalsIgnoreCase(cmd)) {
                return false;
            }

            if (isVersion(cmd)) {
                launcher.log("HeadlessMc - " + Launcher.VERSION);
                return true;
            }

            CommandLineManager clm = launcher.getCommandLine();
            clm.setQuickExitCli(true);
            clm.getCommandConsumer().accept(cmd);
            if (clm.isWaitingForInput()) {
                log.debug("Waiting for more input...");
                clm.read(launcher);
            }

            log.debug("Exiting QuickExitCli");
        }

        return cmd != null;
    }

    private static String collectArgs(Launcher launcher, String... args) {
        boolean quickExitCli = false;
        val cmd = new StringBuilder();
        for (val arg : args) {
            if (arg == null) {
                continue;
            }

            if (!quickExitCli && isVersion(arg)) {
                launcher.log("HeadlessMc - " + Launcher.VERSION);
                return "--version";
            }

            if (quickExitCli) {
                cmd.append(arg).append(" ");
            }

            if (arg.equalsIgnoreCase("--command")) {
                quickExitCli = true;
            }
        }

        return quickExitCli ? cmd.toString().trim() : null;
    }

    private static boolean isVersion(String string) {
        return string.equalsIgnoreCase("--version") || string.equalsIgnoreCase("-version") || string.equalsIgnoreCase("version");
    }

}
