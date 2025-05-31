package io.github.headlesshq.headlessmc.launcher.mods;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.api.Platform;
import io.github.headlesshq.headlessmc.launcher.api.VersionId;
import io.github.headlesshq.headlessmc.launcher.command.download.InstallerService;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class VersionModdableGameProvider implements ModdableGameProvider {
    private final Iterable<Version> versions;
    private final Launcher launcher;

    @Override
    public Iterable<ModdableGame> getGames() {
        List<ModdableGame> moddableGames = new ArrayList<>();
        for (Version version : versions) {
            moddableGames.add(new VersionModdableGame(launcher, version));
        }

        return moddableGames;
    }

    @Override
    public boolean providesOnlyClients() {
        return true;
    }

    @Override
    public void download(Launcher launcher, VersionId version, String... args) throws CommandException {
        if (version.isServer()) {
            throw new CommandException("Failed to download server version " + version);
        }

        InstallerService installerService = new InstallerService(launcher);
        installerService.install(version.getClientCommandArgument(), args);
    }

    @RequiredArgsConstructor
    public static final class VersionModdableGame implements ModdableGame {
        private final Launcher launcher;
        private final Version version;

        @Override
        public Path getModsDirectory() {
            return launcher.getGameDir(version).getBase().toPath().resolve("mods");
        }

        @Override
        public String getVersionName() {
            return version.getParentName() == null ? version.getName() : version.getParentName();
        }

        @Override
        public Platform getPlatform() {
            Modlauncher modlauncher = Modlauncher.getFromVersionName(version.getName());
            return modlauncher == null ? Platform.VANILLA : modlauncher.getPlatform();
        }

        @Override
        public @Nullable String getBuild() {
            return null; // TODO: find a reliable way to get forge/fabric builds from version name
        }

        @Override
        public boolean isServer() {
            return false;
        }

        @Override
        public int getId() {
            return version.getId();
        }

        @Override
        public String getName() {
            return version.getName();
        }
    }

}
