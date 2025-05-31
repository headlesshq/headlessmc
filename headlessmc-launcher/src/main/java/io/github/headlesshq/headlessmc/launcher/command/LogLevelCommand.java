package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.AbstractCommand;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import lombok.CustomLog;

import java.util.Locale;
import java.util.logging.Level;

@CustomLog
public class LogLevelCommand extends AbstractCommand {
    public LogLevelCommand(HeadlessMc ctx) {
        super(ctx, "loglevel", "Set the loglevel of HeadlessMC's logger.");
        args.put("<level>", "One of " + ctx.getLoggingService().getLevels() + ". Decides how much" +
            " log output you see. Warning: lower levels than INFO might" +
            " leak sensitive information!");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        if (args.length < 2) {
            throw new CommandException("Please specify a LogLevel of "
                                           + ctx.getLoggingService().getLevels()
                                           + ".");
        }

        Level level;
        try {
            level = Level.parse(args[1].toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            throw new CommandException("Couldn't set level to '"
                                           + args[1]
                                           + "', please use one of "
                                           + ctx.getLoggingService().getLevels() + ".");
        }

        ctx.getLoggingService().setLevel(level);
        for (Level l : ctx.getLoggingService().getLevels()) {
            log.log(l, "Logging with Level: " + l.getName());
        }
    }

}
