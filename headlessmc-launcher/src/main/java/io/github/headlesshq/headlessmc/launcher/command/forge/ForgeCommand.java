package io.github.headlesshq.headlessmc.launcher.command.forge;

import lombok.CustomLog;
import lombok.Getter;
import lombok.val;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.command.download.AbstractDownloadingVersionCommand;
import io.github.headlesshq.headlessmc.launcher.command.download.ModLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@CustomLog
public class ForgeCommand extends AbstractDownloadingVersionCommand implements ModLauncherCommand {
    // TODO: IndexCache should be available outside of ForgeCommand, we need it for other stuff
    private final ForgeInstaller installer;
    private final ForgeIndexCache cache;

    public ForgeCommand(Launcher ctx, String name, ForgeInstaller installer, ForgeIndexCache cache) {
        super(ctx, name, "Downloads " + name + ".");
        args.put("<version>", "The version to download " + name + " for.");
        args.put("--uid", "Specify a specific " + name + " version.");
        args.put("-refresh", "Refresh index of " + name + " versions.");
        args.put("-list", "List " + name + " versions for the specified version.");
        args.put("-inmemory", "Launch the forge installer inside this JVM.");
        this.installer = installer;
        this.cache = cache;
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        boolean refresh = CommandUtil.hasFlag("-refresh", args);
        if (cache.isEmpty() || refresh) {
            cache.refresh();
        }

        if (args.length < 2 || args.length == 2 && refresh) {
            logTable(cache);
        } else {
            super.execute(line, args);
        }
    }

    public @Nullable ForgeVersion getVersion(Version ver, boolean list, @Nullable String uid, boolean refresh) throws CommandException {
        if (refresh && cache.isEmpty()) {
            cache.refresh();
        }

        val versions = cache.stream()
                .filter(v -> v.getVersion().equals(ver.getName()))
                .filter(v -> uid == null || uid.equalsIgnoreCase(v.getName()))
                .collect(Collectors.toList());

        if (list) {
            logTable(versions);
            return null;
        }

        return versions.stream()
                .findFirst()
                .orElseThrow(() -> new CommandException(
                        "Couldn't find " + installer.getForgeName() + " for version "
                                + ver.getName() + (uid == null
                                ? "!"
                                : " and uid " + uid + "!")));
    }

    @Override
    public void execute(Version ver, String... args) throws CommandException {
        String uid = CommandUtil.getOption("--uid", args);
        if (cache.isEmpty() || CommandUtil.hasFlag("-norecursivedownload", args)) {
            cache.refresh();
        }

        ForgeVersion version = getVersion(ver, CommandUtil.hasFlag("-list", args), uid, false);
        if (version == null) { // logged
            return;
        }

        boolean server = CommandUtil.hasFlag("-server", args);
        if (server) {
            ctx.log("Installing " + installer.getForgeName() + " Server " + version.getFullName());
        } else {
            ctx.log("Installing " + installer.getForgeName() + " " + version.getFullName());
        }

        val uuid = UUID.randomUUID();
        val fm = ctx.getFileManager().createRelative(uuid.toString());
        try {
            boolean inMemory = CommandUtil.hasFlag("-inmemory", args)
                    || ctx.getConfig().get(LauncherProperties.ALWAYS_IN_MEMORY, false);
            if (server) {
                installer.installServer(ver, version, fm, CommandUtil.getOption("--dir", args), inMemory);
            } else {
                installer.install(ver, version, fm, inMemory);
                ctx.getVersionService().refresh();
            }
        } catch (IOException e) {
            val message = "Failed to install " + installer.getForgeName() + " for version " + ver.getName()
                + ": " + e.getMessage();
            log.error(message);
            throw new CommandException(message);
        } finally {
            try {
                ctx.getFileManager().delete(fm.getBase());
            } catch (IOException e) {
                log.error("Couldn't delete " + fm.getBase() + ": " + e);
            }
        }
    }

    private void logTable(Iterable<ForgeVersion> versions) {
        ctx.log(installer.getForgeName() + " versions:");
        ctx.log(new Table<ForgeVersion>()
                    .addAll(versions)
                    .withColumn("version", ForgeVersion::getVersion)
                    .withColumn("uid", ForgeVersion::getName)
                    .build());
    }

    public static ForgeCommand lexforge(Launcher launcher) {
        ForgeInstaller installer = new ForgeInstaller(ForgeRepoFormat.lexForge(), launcher, "Forge", ForgeRepoFormat.LEX_FORGE_URL);
        return new ForgeCommand(launcher, "forge", installer, new ForgeIndexCache(launcher, ForgeIndexCache.LEX_FORGE_INDICES));
    }

    public static ForgeCommand neoforge(Launcher launcher) {
        ForgeInstaller installer = new ForgeInstaller(ForgeRepoFormat.neoForge(), launcher, "NeoForge", ForgeRepoFormat.NEO_FORGE_URL);
        return new ForgeCommand(launcher, "neoforge", installer, new ForgeIndexCache(launcher, ForgeIndexCache.NEO_FORGE_INDICES));
    }

}
