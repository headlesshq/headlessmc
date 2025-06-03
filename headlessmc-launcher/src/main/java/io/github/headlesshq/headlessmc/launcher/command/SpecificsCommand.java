package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModRepository;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.CustomLog;

import java.io.IOException;
import java.util.stream.Collectors;

@CustomLog
public class SpecificsCommand extends AbstractVersionCommand {
    public SpecificsCommand(Launcher ctx) {
        super(ctx, "specifics", "Installs version specific mods.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        if (args.length < 2) {
            ctx.log(new Table<VersionSpecificModRepository>()
                        .withColumn("name", VersionSpecificModRepository::getName)
                        .withColumn("version", VersionSpecificModRepository::getVersion)
                        .withColumn("url", v -> v.getUrl().toString())
                        .addAll(ctx.getVersionSpecificModManager().getSpecificMods())
                        .build());
            return;
        }

        super.execute(line, args);
    }

    @Override
    public void execute(Version version, String... args) throws CommandException {
        if (args.length < 3) {
            String list = ctx.getVersionSpecificModManager().getSpecificMods().stream().map(HasName::getName).collect(Collectors.joining(", "));
            throw new CommandException("Please specify which version specific mod to download (" + list + ").");
        }

        VersionSpecificModRepository repository = ctx.getVersionSpecificModManager().getRepository(args[2]);

        try {
            ctx.getVersionSpecificModManager().download(version, repository);
            ctx.getVersionSpecificModManager().deleteSpecificsOfOtherVersions(version, repository, ctx.getGameDir(version).getDir("mods").toPath());
            ctx.getVersionSpecificModManager().install(version, repository, ctx.getGameDir(version).getDir("mods").toPath());
            ctx.log("Installed " + repository.getName() + "-" + repository.getVersion() + " for " + version.getName() + " successfully.");
        } catch (IOException e) {
            log.debug("Failed to install " + repository.getName() + " for version " + version.getName(), e);
            throw new CommandException("Failed to install " + repository.getName() + " for version " + version.getName() + ": " + e.getMessage());
        }
    }

}
