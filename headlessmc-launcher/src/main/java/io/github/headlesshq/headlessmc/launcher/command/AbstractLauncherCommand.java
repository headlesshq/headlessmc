package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.AbstractCommand;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.Refreshable;

public abstract class AbstractLauncherCommand extends AbstractCommand {
    protected final Launcher ctx;

    public AbstractLauncherCommand(Launcher ctx, String name, String desc) {
        super(ctx, name, desc);
        this.ctx = ctx;
    }

    protected void handleRefresh(Refreshable service, String... args) {
        if (CommandUtil.hasFlag("-refresh", args)) {
            service.refresh();
        }
    }

}
