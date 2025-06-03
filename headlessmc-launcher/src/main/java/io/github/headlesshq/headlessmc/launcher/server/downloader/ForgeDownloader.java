package io.github.headlesshq.headlessmc.launcher.server.downloader;

import com.google.gson.annotations.SerializedName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.download.InstallerService;
import io.github.headlesshq.headlessmc.launcher.command.forge.ForgeCommand;
import io.github.headlesshq.headlessmc.launcher.command.forge.ForgeVersion;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerTypeDownloader;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

@RequiredArgsConstructor
public class ForgeDownloader implements ServerTypeDownloader {
    private final ServerTypeDownloader vanillaDownloader;
    private final Modlauncher modLauncher;

    @Override
    public DownloadHandler download(Launcher launcher, String version, @Nullable String typeVersion, String... args) throws IOException {
        Version parsedVersion = VersionUtil.getVersion(launcher, version);
        InstallerService installerService = new InstallerService(launcher);
        ForgeCommand forgeCommand = (ForgeCommand) installerService.getModLauncherCommand(modLauncher);
        Objects.requireNonNull(forgeCommand);

        String forgeVersion = typeVersion;
        if (forgeVersion == null) {
            try {
                ForgeVersion fv = forgeCommand.getVersion(parsedVersion, false, null, true);
                forgeVersion = Objects.requireNonNull(fv).getName();
            } catch (CommandException e) {
                throw new IOException(e);
            }
        }

        return new ModLauncherCommandDownloader(
                parsedVersion,
                forgeVersion,
                "forge",
                typeVersion,
                args,
                forgeCommand
        );
    }

    @Getter
    private static class BuildData implements Comparable<BuildData> {
        @SerializedName("separator")
        String separator;
        @SerializedName("build")
        int build;
        @SerializedName("maven")
        String maven;
        @SerializedName("version")
        String version;
        @SerializedName("stable")
        boolean stable;

        @Override
        public int compareTo(BuildData other) {
            return Integer.compare(this.build, other.build);
        }
    }

}
