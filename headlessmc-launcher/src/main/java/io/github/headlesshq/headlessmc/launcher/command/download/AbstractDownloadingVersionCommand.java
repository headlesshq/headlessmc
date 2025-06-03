package io.github.headlesshq.headlessmc.launcher.command.download;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.command.AbstractVersionCommand;
import io.github.headlesshq.headlessmc.version.Version;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDownloadingVersionCommand extends AbstractVersionCommand {
    public AbstractDownloadingVersionCommand(Launcher ctx, String name, String desc) {
        super(ctx, name, desc);
    }

    @Override
    public @Nullable Version findObject(boolean byId, boolean byRegex, String versionArg, String... args) throws CommandException {
        Version result = super.findObject(byId, byRegex, versionArg, args);
        if (result == null
                && !byId
                && !byRegex
                && !CommandUtil.hasFlag("-norecursivedownload", args)
                && ctx.getConfig().get(LauncherProperties.AUTO_DOWNLOAD, true)) {
            VersionArgument versionArgument = VersionArgument.parseVersion(versionArg);
            InstallerService installerService = new InstallerService(ctx);
            installerService.install(versionArgument, args);
            result = super.findObject(false, false, versionArg, args);
        }

        return result;
    }

}
