package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.net.Download;
import io.github.headlesshq.headlessmc.net.DownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.parallel.ParallelTaskService;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helps with downloading mod packs in the {@link Mrpack} format.
 * The files of a modpack are downloaded in parallel, see {@link ModpackConfig#parallel()}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ModpackDownloader {
    private final ProgressBarServiceManager progressBarServiceManager;
    private final JsonService jsonService;
    private final FileService fileService;
    private final Holder<ModpackConfig> config;

    public void download(String name, Side side, Path gameDir, DownloadContext context, DownloadBuilder initial) {
        fileService.temp(workingDir -> {
            initial.start(download -> extract(download, workingDir));
            Map<String, Path> files = downloadFiles(context, workingDir, side, name);

            files.forEach((relative, target) -> {
                Path destination = fileService.getPath(gameDir, relative);
                fileService.saveMove(target, destination);
            });

            copyOverrides(workingDir.resolve("overrides"), gameDir);
            switch (side) {
                case CLIENT -> copyOverrides(workingDir.resolve("client-overrides"), gameDir);
                case SERVER -> copyOverrides(workingDir.resolve("server-overrides"), gameDir);
            }
        });
    }

    private void copyOverrides(Path overrides, Path gameDir) {
        if (Files.exists(overrides) && Files.isDirectory(overrides)) {
            fileService.saveMove(overrides, gameDir);
        }
    }

    private Map<String, Path> downloadFiles(DownloadContext context, Path workingDir, Side side, String modpackName) {
        Path index = workingDir.resolve("modrinth.index.json");
        if (!Files.exists(index)) {
            throw new HeadlessMcIOException("Failed to find modrinth.index.json of modpack");
        }

        Mrpack mrpack = jsonService.parse(index, Mrpack.class);
        long totalSize = 0L;
        List<Mrpack.File> filesToDownload = new ArrayList<>(mrpack.files().size());
        for (Mrpack.File file : mrpack.files()) {
            Map<String, String> env = file.env() == null ? new HashMap<>() : file.env();
            String sideType = env.get(side.name().toLowerCase(Locale.ENGLISH));
            if (sideType == null || "required".equalsIgnoreCase(sideType) || "optional".equalsIgnoreCase(sideType)) {
                totalSize += file.fileSize();
                filesToDownload.add(file);
            }
        }

        List<ModpackDownloadTask> tasks = new ArrayList<>(filesToDownload.size());
        for (Mrpack.File file : filesToDownload) {
            if (file.downloads().isEmpty()) {
                throw new HeadlessMcIOException("Failed to find download for file " + file);
            }

            tasks.add(new ModpackDownloadTask(context, file, fileService.getPath(workingDir, file.path())));
        }

        Map<String, Path> files = new HashMap<>();
        if (tasks.isEmpty()) {
            return files;
        }

        try (
            ParallelTaskService<ModpackDownloadTask> taskService = new ParallelTaskService<>(config.get().parallel(), tasks);
            ProgressBar progressBar = progressBarServiceManager.displayProgressBar(new ProgressBar.Configuration(
                "Downloading modpack " + modpackName,
                totalSize,
                ProgressBar.Configuration.Unit.MB
            ))
        ) {
            AtomicInteger finishedTasks = new AtomicInteger();
            taskService.run(task -> {
                if (progressBar.isDummy()) {
                    log.info("Downloaded modpack file {}/{}", finishedTasks.incrementAndGet(), tasks.size());
                }

                // TODO: progressBar has extraMessage
                progressBar.stepBy(task.getSize());
            });
        }

        // the ParallelTaskService throws if any task failed, so every file is downloaded at this point
        for (ModpackDownloadTask task : tasks) {
            files.put(task.path(), task.getTarget());
        }

        return files;
    }

    private void extract(Download download, Path destination) throws HeadlessMcException {
        try (
            InputStream rawInputStream = download.getInputStream();
            ZipInputStream zip = new ZipInputStream(rawInputStream)
        ) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) {
                    throw new HeadlessMcIOException("Zip slip detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // TODO: this is here because of JavaDownloadBuilder Line 188,
            //  this might not read the InputStream completely?!
            rawInputStream.transferTo(new OutputStream() {
                @Override
                public void write(int b) {

                }

                @Override
                public void write(byte @NonNull[] b) throws IOException {

                }

                @Override
                public void write(byte @NonNull[] b, int off, int len) throws IOException {

                }
            });
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to extract modpack  to " + destination, e);
        }
    }

}
