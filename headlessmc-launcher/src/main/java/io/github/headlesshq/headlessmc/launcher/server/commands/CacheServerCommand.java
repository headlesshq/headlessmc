package io.github.headlesshq.headlessmc.launcher.server.commands;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.server.Server;
import lombok.CustomLog;

import java.io.IOException;

@CustomLog
public class CacheServerCommand extends AbstractLauncherCommand implements FindByCommand<Server> {
    public CacheServerCommand(Launcher ctx) {
        super(ctx, "cache", "Cache servers for reuse.");
        args.put("<server>", "The server to cache.");
    }

    @Override
    public void execute(Server server, String... args) throws CommandException {
        try {
            ctx.getServerManager().cache(ctx, server);
            ctx.log("Cached server '" + server.getName() + "'.");
        } catch (IOException e) {
            log.error("Failed to cache server " + server.getPath(), e);
            throw new CommandException(e);
        }
    }

    @Override
    public Iterable<Server> getIterable() {
        return ctx.getServerManager();
    }

}
