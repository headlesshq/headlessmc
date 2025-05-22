package io.github.headlesshq.headlessmc.launcher.command.download;

import lombok.Data;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import org.jetbrains.annotations.Nullable;

@Data
public class VersionArgument implements HasName {
    private final @Nullable Modlauncher modlauncher;
    private final @Nullable String modLauncherVersion;
    private final String name;

    public static VersionArgument parseVersion(String version) throws CommandException {
        String[] split = version.split(":"); // TODO: allow escaped :?
        if (split.length == 1) {
            return new VersionArgument(null, null, version);
        } else {
            Modlauncher modlauncher = Modlauncher.getFromVersionName(split[0]);
            if (modlauncher == null) {
                throw new CommandException("Failed to parse Modlauncher (fabric, neoforge, forge) from: " + split[0]);
            }

            if (split.length == 2) {
                return new VersionArgument(modlauncher, null, split[1]);
            } else if (split.length == 3) {
                return new VersionArgument(modlauncher, split[2], split[1]);
            } else {
                throw new CommandException("To many ':' specified, expected format <version> or <modloader>:<version> (:<modlauncher version>), but was: " + version);
            }
        }
    }

}
