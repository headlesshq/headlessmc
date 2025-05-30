package io.github.headlesshq.headlessmc.launcher.command;

import lombok.val;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.download.AssetsDownloader;
import io.github.headlesshq.headlessmc.launcher.version.Library;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command
public class IntegrityCommand {
    private final Launcher ctx;

    public IntegrityCommand(Launcher ctx) {
        super(ctx, "integrity", "Checks the integrity of the libraries of a version.");
    }

    @Override
    public void execute(Version version, String... args) throws CommandException {
        try {
            int failed = 0;
            int successful = 0;
            int notThere = 0;
            // TODO: make this a service instead
            for (Library library : version.getLibraries()) {
                String libPath = library.getPath(ctx.getProcessFactory().getOs());
                Path path = Paths.get(ctx.getProcessFactory().getFiles().getDir("libraries") + File.separator + libPath);
                if (Files.exists(path)) {
                    ctx.log("Checking " + libPath);
                    if (!ctx.getSha1Service().checkIntegrity(path, library.getSize(), library.getSha1())) {
                        ctx.log("Integrity check failed! Deleting " + libPath);
                        failed++;
                        continue;
                    }

                    successful++;
                } else {
                    ctx.log("Failed to find " + libPath);
                    notThere++;
                }
            }

            int[] values = { failed, successful, notThere };
            checkAssets(version, values, args);
            ctx.log("Integrity check finished, " + values[0] + " failed, " + values[1] + " successful and " + values[2] + " not found.");
            if (ctx.getProcessFactory().getOs().isArm()) {
                ctx.log("Your processor uses ARM, integrity check might not work properly.");
            }
        } catch (IOException e) {
            throw new CommandException("Failed to check integrity: " + e.getMessage());
        }
    }

    private void checkAssets(Version version, int[] values, String...args) throws IOException {
        if (CommandUtil.hasFlag("-assets", args)) {
            ctx.log("Checking assets of version " + version.getName());
            AssetsDownloader assetsDownloader = new AssetsDownloader(ctx.getCommandLine(), ctx.getDownloadService(), ctx, ctx.getMcFiles(), version.getAssetsUrl(), version.getAssets()) {
                @Override
                protected void downloadAsset(String progress, String name, String hash, @Nullable Long size, boolean mapToResources) throws IOException {
                    val firstTwo = hash.substring(0, 2);
                    val to = ctx.getMcFiles().getDir("assets").toPath().resolve("objects").resolve(firstTwo).resolve(hash);
                    Path file = getAssetsFile(name, to, hash, size);
                    copyToLegacy(name, file, hash, size, false);
                    mapToResources(name, file, mapToResources, hash, size, false);
                }

                @Override
                protected boolean shouldCheckFileHash() {
                    return true;
                }

                @Override
                protected boolean integrityCheck(String type, Path file, String hash, @Nullable Long size) throws IOException {
                    if (!Files.exists(file)) {
                        values[2]++; // notThere
                        return true;
                    } else if (super.integrityCheck(type, file, hash, size)) {
                        values[1]++; // successful
                        return true;
                    } else {
                        values[0]++; // unsuccessful
                        return false;
                    }
                }
            };

            assetsDownloader.download();
        }
    }

}
