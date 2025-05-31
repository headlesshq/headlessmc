package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.version.VersionUtil;
import lombok.val;

public class VersionsCommand extends AbstractLauncherCommand {
    public VersionsCommand(Launcher launcher) {
        super(launcher, "versions", "Lists all Minecraft versions.");
        args.putAll(VersionTypeFilter.getArgs());
    }

    @Override
    public void execute(String line, String... args) {
        if (CommandUtil.hasFlag("-refresh", args)) {
            ctx.getVersionService().refresh();
        }

        val filter = VersionTypeFilter.forVersions();
        val versions = filter.apply(ctx.getVersionService().getContents(), args);
        ctx.log(VersionUtil.makeTable(versions));
    }

}
