package io.github.headlesshq.headlessmc.launcher.server.commands;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.server.Server;
import lombok.CustomLog;

import java.io.IOException;

@CustomLog
public class RemoveServerCommand extends AbstractLauncherCommand implements FindByCommand<Server> {
    public RemoveServerCommand(Launcher ctx) {
        super(ctx, "remove", "Remove a server.");
    }

    @Override
    public void execute(Server server, String... args) throws CommandException {
        try {
            ctx.getServerManager().remove(server);
            ctx.log(String.format("Removed server '%s'.", server.getName()));
        } catch (IOException e) {
            log.error(e);
            throw new CommandException(e);
        }
    }

    @Override
    public Iterable<Server> getIterable() {
        return ctx.getServerManager();
    }

}
