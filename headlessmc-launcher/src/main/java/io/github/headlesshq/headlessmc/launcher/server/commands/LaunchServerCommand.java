package io.github.headlesshq.headlessmc.launcher.server.commands;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLaunchProcessLifecycle;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchException;
import io.github.headlesshq.headlessmc.launcher.server.Server;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import lombok.CustomLog;

import java.io.IOException;
import java.nio.file.Path;

@CustomLog
public class LaunchServerCommand extends AbstractLauncherCommand implements FindByCommand<Server> {
    public LaunchServerCommand(Launcher ctx) {
        super(ctx, "launch", "Start a server.");
    }

    @Override
    public void execute(Server server, String... args) throws CommandException {
        ServerLaunchProcessLifecycle lifecycle = new ServerLaunchProcessLifecycle(server, args);
        lifecycle.run(server);
    }

    @Override
    public Iterable<Server> getIterable() {
        return ctx.getServerManager();
    }

    private class ServerLaunchProcessLifecycle extends AbstractLaunchProcessLifecycle {
        private final ServerLauncher serverLauncher;
        private final Server server;

        public ServerLaunchProcessLifecycle(Server server, String[] args) {
            super(LaunchServerCommand.this.ctx, args);
            this.serverLauncher = new ServerLauncher(ctx, server, args);
            this.server = server;
        }

        @Override
        protected Path getGameDir() {
            return server.getPath();
        }

        @Override
        protected Process createProcess() throws LaunchException, CommandException, IOException {
            serverLauncher.setPrepare(prepare);
            serverLauncher.setQuit(quit);
            return serverLauncher.launch();
        }
    }

}
