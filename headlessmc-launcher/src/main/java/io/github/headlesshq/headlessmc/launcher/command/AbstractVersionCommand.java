package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionArgument;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import io.github.headlesshq.headlessmc.launcher.version.family.FamilyUtil;
import io.github.headlesshq.headlessmc.launcher.version.family.HasParent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public abstract class AbstractVersionCommand extends AbstractLauncherCommand implements FindByCommand<Version> {
    public AbstractVersionCommand(Launcher ctx, String name, String desc) {
        super(ctx, name, desc);
    }

    @Override
    public @Nullable Version findObject(boolean byId, boolean byRegex, String versionArg, String... args) throws CommandException {
        Version result = FindByCommand.super.findObject(byId, byRegex, versionArg, args);
        if (result == null) {
            VersionArgument versionArgument = VersionArgument.parseVersion(versionArg);
            return find(versionArgument, getIterable());
        }

        return result;
    }

    @Override
    public Iterable<Version> getIterable() {
        return ctx.getVersionService();
    }

    // completions for new version format?

    // for testing
    protected <V extends HasName & HasParent<V>> @Nullable V find(VersionArgument versionArgument, Iterable<V> versions) {
        String mlVersion = versionArgument.getModLauncherVersion() == null ? null : versionArgument.getModLauncherVersion().toLowerCase(Locale.ENGLISH);
        for (V version : versions) {
            if (Modlauncher.getFromVersionName(version.getName()) == versionArgument.getModlauncher()
                    && (mlVersion == null || version.getName().toLowerCase(Locale.ENGLISH).contains(mlVersion))
                    && (version.getName().equalsIgnoreCase(versionArgument.getName())
                    || FamilyUtil.anyMemberMatches(version, parent -> parent.getName().equalsIgnoreCase(versionArgument.getName())))) {
                return version;
            }
        }

        return null;
    }

}
