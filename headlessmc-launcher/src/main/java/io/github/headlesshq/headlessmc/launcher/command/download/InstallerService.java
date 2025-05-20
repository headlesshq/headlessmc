package io.github.headlesshq.headlessmc.launcher.command.download;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.FabricCommand;
import io.github.headlesshq.headlessmc.launcher.command.forge.ForgeCommand;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class InstallerService {
    private final Map<Modlauncher, ModLauncherCommand> modLauncherCommands = new EnumMap<>(Modlauncher.class);
    private final Launcher launcher;
    private final DownloadCommand downloadCommand;

    public InstallerService(Launcher launcher) {
        this.launcher = launcher;
        // TODO: this is super bad!!!
        this.downloadCommand = findCommand(launcher, DownloadCommand.class, DownloadCommand::new);
        this.modLauncherCommands.put(Modlauncher.FABRIC, findCommand(launcher, FabricCommand.class, FabricCommand::new));
        this.modLauncherCommands.put(Modlauncher.LEXFORGE, findCommand(launcher, ForgeCommand.class, ForgeCommand::lexforge,
                c -> "lexforge".equalsIgnoreCase(c.getName()) || "forge".equalsIgnoreCase(c.getName())));
        this.modLauncherCommands.put(Modlauncher.NEOFORGE, findCommand(launcher, ForgeCommand.class, ForgeCommand::neoforge,
                c -> "neoforge".equalsIgnoreCase(c.getName())));
    }

    public @Nullable ModLauncherCommand getModLauncherCommand(Modlauncher modlauncher) {
        return modLauncherCommands.get(modlauncher);
    }

    public void install(VersionArgument versionArgument, String... args) throws CommandException {
        Version vanilla = launcher.getVersionService().getVersionByName(versionArgument.getName());
        if (vanilla == null) {
            downloadCommand.download(versionArgument.getName());
        }

        installModLauncher(versionArgument, args);
    }

    public void installModLauncher(VersionArgument versionArgument, String... args) throws CommandException {
        if (versionArgument.getModlauncher() == null) {
            return;
        }

        ModLauncherCommand command = modLauncherCommands.get(versionArgument.getModlauncher());
        if (command == null) {
            throw new CommandException("Unknown mod launcher " + versionArgument.getModlauncher());
        }

        Version vanilla = launcher.getVersionService().getVersionByName(versionArgument.getName());
        if (vanilla == null) {
            throw new CommandException("Failed to find version " + versionArgument.getName());
        }

        command.install(vanilla, versionArgument.getModLauncherVersion(), CommandUtil.hasFlag("-modlauncherInMemory", args));
    }

    private <C extends Command> @Nullable C findCommand(Launcher launcher, Class<C> clazz, Function<Launcher, C> fallback) {
        return findCommand(launcher, clazz, fallback, null);
    }

    private <C extends Command> @Nullable C findCommand(Launcher launcher, Class<C> clazz, Function<Launcher, C> fallback, @Nullable Predicate<C> predicate) {
        for (Command command : launcher.getCommandLine().getCommandContext()) {
            if (clazz.isAssignableFrom(command.getClass())) {
                if (predicate != null && !predicate.test((clazz.cast(command)))) {
                    continue;
                }

                return clazz.cast(command);
            }
        }

        return fallback.apply(launcher);
    }

}
