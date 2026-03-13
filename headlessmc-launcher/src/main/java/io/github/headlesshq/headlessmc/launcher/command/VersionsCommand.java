package io.github.headlesshq.headlessmc.launcher.command;

import lombok.val;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.version.VersionUtil;

public class VersionsCommand extends AbstractLauncherCommand {
    public VersionsCommand(Launcher launcher) {
        super(launcher, "versions", "Lists all Minecraft versions.");
        args.putAll(VersionTypeFilter.getArgs());
    }

    @Override
    public void execute(String line, String... args) {
        handleRefresh(ctx.getVersionService(), args);

        val filter = VersionTypeFilter.forVersions();
        val versions = filter.apply(ctx.getVersionService().getContents(), args);
        ctx.log(VersionUtil.makeTable(versions));
    }

}
