package io.github.headlesshq.headlessmc.api.command.line;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.process.InAndOutProvider;

import java.io.Console;
import java.io.IOError;

/**
 * An implementation of {@link CommandLineReader} that reads from the console supplied by {@link InAndOutProvider}{@code .getConsole()}.
 * This is usually {@link System#console()}.
 */
@CustomLog
@RequiredArgsConstructor
class ConsoleCommandLineReader implements CommandLineReader {
    private final Console console;

    @Override
    public void read(HeadlessMc hmc) throws IOError {
        String line;
        CommandLineManager ctx = hmc.getCommandLine();
        while ((line = ctx.isHidingPasswords() ? String.valueOf(console.readPassword()) : console.readLine()) != null) {
            ctx.getCommandConsumer().accept(line);
            if (ctx.isQuickExitCli()) {
                if (!ctx.isWaitingForInput()) {
                    break;
                }

                log.debug("Waiting for more input...");
            }
        }
    }

}
