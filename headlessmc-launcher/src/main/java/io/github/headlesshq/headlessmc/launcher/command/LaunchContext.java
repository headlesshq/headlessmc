package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.CommandContextImpl;
import io.github.headlesshq.headlessmc.api.command.PasswordCommand;
import io.github.headlesshq.headlessmc.api.command.impl.HelpCommand;
import io.github.headlesshq.headlessmc.api.command.impl.MemoryCommand;
import io.github.headlesshq.headlessmc.api.command.impl.MultiCommand;
import io.github.headlesshq.headlessmc.api.command.impl.QuitCommand;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.download.DownloadCommand;
import io.github.headlesshq.headlessmc.launcher.command.forge.ForgeCommand;
import io.github.headlesshq.headlessmc.launcher.command.login.LoginCommand;
import io.github.headlesshq.headlessmc.launcher.mods.command.ModCommand;
import io.github.headlesshq.headlessmc.launcher.server.ServerCommand;

public class LaunchContext extends CommandContextImpl {
    public LaunchContext(Launcher ctx) {
        this(ctx, true);
    }

    public LaunchContext(Launcher ctx, boolean addLoginCommand) {
        super(ctx);
        add(new LaunchCommand(ctx));
        add(new QuitCommand(ctx));
        add(new FabricCommand(ctx));
        add(new IntegrityCommand(ctx));
        add(ForgeCommand.lexforge(ctx));
        add(ForgeCommand.neoforge(ctx));
        add(new JsonCommand(ctx));
        add(new HelpCommand(ctx));
        add(new JavaCommand(ctx));
        add(new MemoryCommand(ctx));
        add(new VersionsCommand(ctx));
        add(new LogLevelCommand(ctx));
        add(new ConfigCommand(ctx));
        add(new PasswordCommand(ctx));
        add(new ServerCommand(ctx));
        add(ModCommand.forClientVersions(ctx));
        if (addLoginCommand) {
            add(new LoginCommand(ctx));
            add(new io.github.headlesshq.headlessmc.launcher.command.login.YggdrasilLoginCommand(ctx));
        }

        add(new AccountsCommand(ctx));
        add(new DownloadCommand(ctx));
        add(new SpecificsCommand(ctx));
        add(new OfflineCommand(ctx));
        add(new PluginsCommand(ctx));
        add(new MultiCommand(ctx));
    }

}
