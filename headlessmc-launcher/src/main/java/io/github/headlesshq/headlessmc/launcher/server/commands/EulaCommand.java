package io.github.headlesshq.headlessmc.launcher.server.commands;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchException;
import io.github.headlesshq.headlessmc.launcher.server.Server;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import lombok.CustomLog;

import java.io.IOException;

@CustomLog
public class EulaCommand extends AbstractLauncherCommand implements FindByCommand<Server> {
    public EulaCommand(Launcher ctx) {
        super(ctx, "eula", "Handle the EULA of a server.");
        args.put("<server>", "The server to handle.");
        args.put("accept", "If you want to accept the EULA.");
    }

    @Override
    public void execute(Server server, String... args) throws CommandException {
        try {
            ServerLauncher serverLauncher = new ServerLauncher(ctx, server, args);
            serverLauncher.eulaLaunch();

            if (args.length > 1 && (
                    CommandUtil.hasFlag("accept", args)
                        || CommandUtil.hasFlag("-accept", args)
                        || CommandUtil.hasFlag("--accept", args))) {
                serverLauncher.acceptEula();
                ctx.log("EULA accepted.");
            } else {
                ctx.log(serverLauncher.readEula());
            }
        } catch (IOException | LaunchException e) {
            log.error(e);
            throw new CommandException("Failed to read EULA of server " + server.getName(), e);
        }
    }

    @Override
    public Iterable<Server> getIterable() {
        return ctx.getServerManager();
    }

}
