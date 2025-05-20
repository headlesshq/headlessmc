package io.github.headlesshq.headlessmc.api.command.line;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.process.InAndOutProvider;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The simplest {@link CommandLineReader} implementation,
 * which reads {@link InAndOutProvider}{@code .getIn()} with a {@link BufferedReader}.
 */
@CustomLog
public class BufferedCommandLineReader implements CommandLineReader {
    @Override
    public void read(HeadlessMc hmc) throws IOError {
        CommandLineManager ctx = hmc.getCommandLine();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(ctx.getInAndOutProvider().getIn().get()))) {
            String line;
            while ((line = in.readLine()) != null) {
                ctx.getCommandConsumer().accept(line);
                if (ctx.isQuickExitCli()) {
                    if (!ctx.isWaitingForInput()) {
                        break;
                    }

                    log.debug("Waiting for more input...");
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

}
