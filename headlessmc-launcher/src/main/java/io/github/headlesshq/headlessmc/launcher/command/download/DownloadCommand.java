package io.github.headlesshq.headlessmc.launcher.command.download;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.api.command.YesNoContext;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.command.AbstractLauncherCommand;
import io.github.headlesshq.headlessmc.launcher.command.VersionTypeFilter;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.CustomLog;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@CustomLog
public class DownloadCommand extends AbstractLauncherCommand
    implements FindByCommand<VersionInfo> {
    private final Launcher ctx;
    private final AbstractDownloadingVersionCommand downloadCommand;

    public DownloadCommand(Launcher ctx) {
        super(ctx, "download", "Downloads a version.");
        this.downloadCommand = new AbstractDownloadingVersionCommand(ctx, "download", "Downloads a version.") {
            @Override
            public void execute(Version obj, String... args) {
                // NOP
            }
        };

        args.put("<version/id>", "The name/id of the version to download." +
                " If you use the id you also need to use the -id flag.");
        args.put("-id", "If you specified the version via id you need to add this flag.");
        args.put("-refresh", "Refresh the list of Minecraft versions.");
        args.putAll(VersionTypeFilter.getArgs());
    }

    @Deprecated
    @SuppressWarnings("unused")
    public DownloadCommand(Launcher ctx, VersionInfoCache versionInfoCache) {
        this(ctx);
    }

    @Override
    public void execute(VersionInfo version, String... args)
        throws CommandException {
        val file = new File(ctx.getMcFiles().getDir("versions") + File.separator
                                + version.getName() + File.separator
                                + version.getName() + ".json");
        if (file.exists()) {
            if (CommandUtil.hasFlag("-noredownload", args)) {
                return;
            }

            ctx.log(version.getName() + " has already been downloaded, download anyways? (y/n)");
            YesNoContext.goBackAfter(ctx, r -> {
                if (r) {
                    download(version, file);
                }
            });
        } else {
            download(version, file);
        }
    }

    private void download(VersionInfo version, File file)
        throws CommandException {
        ctx.log("Downloading " + version.getName() + "...");
        try {
            log.debug("Downloading version " + version.getName() + " from "
                          + version.getUrl() + " to " + file.getAbsolutePath());
            ctx.getDownloadService().download(version.getUrl(), file.toPath());
            ctx.getVersionService().refresh();
            ctx.log("Download successful!");
        } catch (IOException ioe) {
            throw new CommandException("Couldn't download version "
                                           + version.getName() + " from "
                                           + version.getUrl() + ": "
                                           + ioe.getMessage());
        }
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        /*
            !!!

            Don't use lomboks val inside method. Compilation will fail with an
            AssertionError. This bug seems to be exclusive to crosscompiling so
            I didn't report it. The cause seems to be the 'FindByCommand.super'.

            !!!
         */
        Collection<VersionInfo> versions =
            ctx.getVersionInfoCache().cache(CommandUtil.hasFlag("-refresh", args));
        if (args.length < 2 || super.args.containsKey(args[1].toLowerCase(Locale.ENGLISH))) {
            Collection<VersionInfo> filtered =
                new VersionTypeFilter<>(VersionInfo::getType)
                    .apply(versions, args);

            ctx.log(new Table<VersionInfo>()
                        .addAll(filtered)
                        .withColumn("id", v -> String.valueOf(v.getId()))
                        .withColumn("name", VersionInfo::getName)
                        .withColumn("type", VersionInfo::getType)
                        .build());
        } else {
            FindByCommand.super.execute(line, args);
        }
    }

    @Override
    public void onObjectNotFound(boolean byId, boolean byRegex, String objectArg, String... args) throws CommandException {
        if (CommandUtil.hasFlag("-norecursivedownload", args) || downloadCommand.findObject(byId, byRegex, objectArg, args) == null) {
            FindByCommand.super.onObjectNotFound(byId, byRegex, objectArg, args);
        }
    }

    @Override
    public Iterable<VersionInfo> getIterable() {
        return ctx.getVersionInfoCache();
    }

    @Override
    public void getCompletions(String line, List<Map.Entry<String, @Nullable String>> completions, String... args) {
        if (args.length == 2 && !ctx.getVersionInfoCache().isCached()) {
            ctx.getVersionInfoCache().cache(false);
        }

        FindByCommand.super.getCompletions(line, completions, args);
    }

    public void download(String version) throws CommandException {
        // this is really bad...
        execute("download " + version + " -noredownload -norecursivedownload", "download", version, "-noredownload", "-norecursivedownload");
    }

}
