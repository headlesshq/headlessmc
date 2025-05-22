package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.Application;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;

class BufferedCommandLineReader implements CommandLineReader {
    @Override
    public void read(Application application) throws IOError {
        CommandLineManager commandLine = application.getCommandLine();
        CommandContext context = commandLine.getInteractiveContext();
        if (context == null) {
            return;
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(commandLine.getStdIO().getIn().get()))) {
            String line;
            while ((line = in.readLine()) != null) {
                synchronized (application.getLock()) {
                    context = commandLine.getInteractiveContext();
                    if (context == null) {
                        return;
                    }

                    context.execute(line);
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

}
