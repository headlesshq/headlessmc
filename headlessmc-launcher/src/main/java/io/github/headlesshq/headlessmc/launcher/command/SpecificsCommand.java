package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModRepository;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import lombok.CustomLog;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@CustomLog
public class SpecificsCommand extends AbstractVersionCommand {
    public SpecificsCommand(Launcher ctx) {
        super(ctx, "specifics", "Installs version specific mods.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        if (args.length < 2) {
            // would be better to fetch versions one by one and print them one by one
            // so that the user gets immediate feedback
            Map<VersionSpecificModRepository, String> versions = fetchVersions();
            ctx.log(new Table<VersionSpecificModRepository>()
                    .withColumn("name", VersionSpecificModRepository::getName)
                    .withColumn("version", repo -> versions.getOrDefault(repo, "-"))
                    .withColumn("url", v -> cleanUrl(v.getUrl()))
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
            ctx.log("Installed " + repository.getName() + "-" + repository.getVersion(ctx.getDownloadService()) + " for " + version.getName() + " successfully.");
        } catch (IOException e) {
            log.debug("Failed to install " + repository.getName() + " for version " + version.getName(), e);
            throw new CommandException("Failed to install " + repository.getName() + " for version " + version.getName() + ": " + e.getMessage());
        }
    }

    private Map<VersionSpecificModRepository, String> fetchVersions() {
        Map<VersionSpecificModRepository, String> versions = new ConcurrentHashMap<>();
        ctx.getVersionSpecificModManager().getSpecificMods().stream().parallel().forEach(repository -> {
            try {
                String version = repository.getVersion(ctx.getDownloadService());
                versions.put(repository, version);
            } catch (IOException e) {
                log.error("Failed to get latest release for " + repository.getName() + "/" + repository.getOwner());
                versions.put(repository, "-");
            }
        });

        return versions;
    }

    private String cleanUrl(URL url) {
        String result = url.toString();
        if (result.endsWith("/releases/download/")) {
            result = result.substring(0, result.length() - "/releases/download/".length());
        }

        return result;
    }

}
