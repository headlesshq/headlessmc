package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import picocli.CommandLine;

public class YesNoContext extends CommandContextImpl {
    private final YesNoCallback callback;
    private final HeadlessMc ctx;

    // TODO
    public YesNoContext(YesNoCallback callback, HeadlessMc ctx) {
        this(new CommandLine(null), callback, ctx);
    }

    public YesNoContext(CommandLine picocli, YesNoCallback callback, HeadlessMc ctx) {
        super(picocli);
        this.callback = callback;
        this.ctx = ctx;
    }

    public static void goBackAfter(HeadlessMc ctx, YesNoCallback callback) {
        ctx.getCommandLine().setWaitingForInput(true);
        CommandContext current = ctx.getCommandLine().getCommandContext();
        ctx.getCommandLine().setCommandContext(new YesNoContext(result -> {
            try {
                ctx.getCommandLine().setWaitingForInput(false);
                callback.accept(result);
            } finally {
                ctx.getCommandLine().setCommandContext(current);
            }
        }, ctx));
    }

    @Override
    public void execute(String command) {
        try {
            if ("y".equalsIgnoreCase(command)) {
                callback.accept(true);
            } else if ("n".equalsIgnoreCase(command)) {
                callback.accept(false);
            } else {
                ctx.log("Expected one of y/n...");
            }
        } catch (CommandException ce) {
            ctx.log(ce.getMessage());
        }
    }

}
