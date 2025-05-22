package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.plugin.HeadlessMcPlugin;

public class PluginsCommand extends AbstractLauncherCommand {
    public PluginsCommand(Launcher ctx) {
        super(ctx, "plugins", "Lists installed plugins.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        // TODO: list TransformerPlugins
        ctx.log(new Table<HeadlessMcPlugin>()
            .withColumn("name", HasName::getName)
            .withColumn("description", HasDescription::getDescription)
            .addAll(ctx.getPluginManager().getPlugins())
            .build());
    }

}
