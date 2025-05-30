package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.config.PropertyTypes;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: this!
public class ConfigCommand extends AbstractLauncherCommand implements FindByCommand<Config> {
    public ConfigCommand(Launcher ctx) {
        super(ctx, "config", "Manage your configs.");
        args.put("-refresh", "Reloads all configs from the disk.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        String property = CommandUtil.getOption("--property", args);
        if (property != null) {
            if (property.contains("=")) {
                String[] split = property.split("=", 2);
                String value = split.length > 1 ? split[1] : "";
                System.setProperty(split[0], value);
                ctx.log("Set property " + split[0] + " to " + value);
            } else {
                ctx.log(property + " = " + ctx.getConfig().get(PropertyTypes.string(property)));
            }

            return;
        } else if (CommandUtil.hasFlag("--property", args)) {
            Map<String, String> map = new HashMap<>();
            System.getProperties().forEach((key, value) -> map.put(String.valueOf(key), String.valueOf(value)));
            List<Map.Entry<String, String>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Map.Entry.comparingByKey());
            ctx.log(new Table<Map.Entry<String, String>>()
                    .withColumn("name", Map.Entry::getKey)
                    .withColumn("value", Map.Entry::getValue)
                    .addAll(entries)
                    .build());
            return;
        }

        if (CommandUtil.hasFlag("-refresh", args)) {
            ctx.getConfigService().refresh();
        }

        if (args.length == 1) {
            ctx.log(new Table<Config>()
                        .withColumn("id", c -> String.valueOf(c.getId()))
                        .withColumn("name", HasName::getName)
                        .addAll(ctx.getConfigService())
                        .build());
        } else if (!(args.length > 1 && args[1].equalsIgnoreCase("-refresh"))) {
            FindByCommand.super.execute(line, args);
        }
    }

    @Override
    public void execute(Config obj, String... args) {
        ctx.log("Loading config " + obj.getName() + "...");
        ctx.getConfigService().setConfig(obj);
    }

    @Override
    public Iterable<Config> getIterable() {
        return ctx.getConfigService();
    }

}
