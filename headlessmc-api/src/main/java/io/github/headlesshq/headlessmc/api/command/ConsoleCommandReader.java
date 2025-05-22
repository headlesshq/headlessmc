package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.Application;
import lombok.Getter;
import lombok.Setter;

import java.io.Console;
import java.io.IOError;
import java.io.IOException;

import static java.lang.String.valueOf;

@Getter
@Setter
class ConsoleCommandReader implements CommandLineReader, HidesPasswords {
    private volatile boolean hidingPasswords = false;

    @Override
    public void read(Application application) throws IOError {
        CommandLineManager commandLine = application.getCommandLine();
        Console console = commandLine.getStdIO().getConsole().get();
        if (console == null) {
            throw new IOError(new IOException("System.console was null!"));
        }

        CommandContext context = commandLine.getInteractiveContext();
        if (context == null) {
            return;
        }

        String line;
        while ((line = isHidingPasswords() ? valueOf(console.readPassword()) : console.readLine()) != null) {
            synchronized (application.getLock()) {
                context = commandLine.getInteractiveContext();
                if (context == null) {
                    return;
                }

                context.execute(line);
            }
        }
    }

}
