package io.github.headlesshq.headlessmc.launcher.command.download;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.version.Version;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface ModLauncherCommand extends FindByCommand<Version> {
    default void install(Version version, @Nullable String uid, boolean inMemory) throws CommandException {
        List<String> args = new ArrayList<>();
        args.add(getName());
        args.add(version.getName());
        args.add("-norecursivedownload");
        if (inMemory) {
            args.add("-inmemory");
        }

        if (uid != null) {
            args.add("--uid");
            args.add(uid);
        }

        execute(version, args.toArray(new String[0]));
    }

}
