package io.github.headlesshq.headlessmc.launcher.server.commands;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.server.Server;

public class ListServersCommand extends AbstractLauncherCommand {
    public ListServersCommand(Launcher ctx) {
        super(ctx, "list", "List installed servers.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        if (CommandUtil.hasFlag("-refresh", args)) {
            ctx.getServerManager().refresh();
        }

        ctx.log(
            new Table<Server>()
                    .withColumn("id", server -> String.valueOf(server.getId()))
                    .withColumn("type", server -> server.getVersion().getServerType().getName())
                    .withColumn("version", server -> server.getVersion().getVersion())
                    .withColumn("name", HasName::getName)
                    .addAll(ctx.getServerManager().getContents())
                    .build()
        );
    }

}
