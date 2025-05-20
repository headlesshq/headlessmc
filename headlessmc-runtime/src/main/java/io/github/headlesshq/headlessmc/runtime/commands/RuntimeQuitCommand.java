package io.github.headlesshq.headlessmc.runtime.commands;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.command.YesNoContext;
import io.github.headlesshq.headlessmc.api.command.QuitCommand;
import io.github.headlesshq.headlessmc.runtime.RuntimeProperties;

public class RuntimeQuitCommand extends QuitCommand {
    public RuntimeQuitCommand(HeadlessMc ctx) {
        super(ctx);
        args.put("-y", "Will not ask you if you want to quit.");
    }

    @Override
    public void execute(String line, String... args) {
        if (CommandUtil.hasFlag("-y", args) || ctx.getConfig().get(RuntimeProperties.DONT_ASK_FOR_QUIT, false)) {
            super.execute(line, args);
        } else {
            ctx.log("Minecraft won't save properly. Quit anyways (Y/N)?");
            YesNoContext.goBackAfter(ctx, result -> {
                if (result) {
                    super.execute(line, args);
                }
            });

        }
    }

    @Override
    public String getDescription() {
        return "Quits the game.";
    }

}
