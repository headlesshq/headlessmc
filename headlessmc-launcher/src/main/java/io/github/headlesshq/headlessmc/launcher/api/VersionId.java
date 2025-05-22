package io.github.headlesshq.headlessmc.launcher.api;

import lombok.Data;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionArgument;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import org.jetbrains.annotations.Nullable;

/**
 * {@link VersionArgument} but also allows to specify server as type.
 */
@Data
public class VersionId implements HasName {
    private final Platform platform;
    private final @Nullable String build;
    private final boolean server;
    private final String name;

    public boolean isServer() {
        return server || platform.isOnlyServer();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (server && !platform.isOnlyServer()) {
            builder.append("server:");
        } else if (platform == Platform.VANILLA) {
            builder.append(name);
            return builder.toString();
        }

        builder.append(platform.getName()).append(":").append(name);
        if (build != null) {
            builder.append(":").append(build);
        }

        return builder.toString();
    }

    public VersionArgument getClientCommandArgument() {
        Modlauncher modlauncher = Modlauncher.getFromVersionName(platform.getName());
        if (modlauncher != null) {
            return new VersionArgument(modlauncher, build, name);
        }

        return new VersionArgument(null, null, name);
    }

    public static VersionId parse(String version) throws CommandException {
        String[] split = version.split(":"); // TODO: allow escaped :?
        if (split.length == 1) {
            return new VersionId(Platform.VANILLA, null, false,version);
        } else {
            boolean server = false;
            int platformIndex = 0;
            int versionIndex = 1;
            int buildIndex = 2;
            if ("server".equalsIgnoreCase(split[0])) {
                if (split.length == 2) {
                    throw new CommandException("Please specify server:<platform>:<version>");
                }

                server = true;
                platformIndex = 1;
                versionIndex = 2;
                buildIndex = 3;
            }

            Platform platform = Platform.getPlatform(split[platformIndex]);
            if (platform == null) {
                throw new CommandException("Unsupported platform: " + split[platformIndex]);
            }

            if (server && !platform.isServer()) {
                throw new CommandException(platform.getName() + " does not support servers!");
            }

            String name = split[versionIndex];
            String build = null;
            if (split.length == buildIndex + 1) {
                build = split[buildIndex];
            } else if (split.length >= buildIndex + 1) {
                throw new CommandException("To many ':' specified. Max is server:<platform>:<version>:<build>.");
            }

            return new VersionId(platform, build, server || platform.isOnlyServer(), name);
        }
    }

}
