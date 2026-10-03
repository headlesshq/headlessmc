package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.parallel.ParallelTaskService;
import io.github.headlesshq.headlessmc.net.parallel.TaskObserver;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LibraryDownloader {
    private final ProgressbarService progressbarService;
    private final DownloadService downloadService;
    private final Holder<LibraryConfig> config;

    public void download(List<MavenRepository> repositories, List<LibraryFile> files) {
        try (DownloadContext context = downloadService.context()) {
            List<LibraryDownloadTask> tasks = createTasks(context, files);
            if (tasks.isEmpty()) {
                return;
            }

            try (
                ParallelTaskService<LibraryDownloadTask> taskService = new ParallelTaskService<>(config.get().parallel(), tasks);
                ProgressBar progressBar = progressbarService.displayProgressBar(new ProgressBar.Configuration(
                    "Downloading libraries",
                    taskService.getTotalSize(),
                    ProgressBar.Configuration.Unit.MB
                ))
            ) {
                AtomicInteger finishedTasks = new AtomicInteger();
                taskService.run(new TaskObserver<>() {
                    @Override
                    public void onTaskCompleted(LibraryDownloadTask task) {
                        if (progressBar.isDummy()) {
                            log.info("Downloaded library {}/{}", finishedTasks.incrementAndGet(), tasks.size());
                        }

                        progressBar.stepBy(task.getSize());
                    }

                    @Override
                    public void onTaskFailed(LibraryDownloadTask task, Exception exception) {
                        task.retry(repositories);
                        if (progressBar.isDummy()) {
                            log.info("Downloaded library {}/{} (retry)", finishedTasks.incrementAndGet(), tasks.size());
                        }

                        progressBar.stepBy(task.getSize());
                    }
                });
            }
        }
    }

    private List<LibraryDownloadTask> createTasks(DownloadContext context, List<LibraryFile> files) {
        List<LibraryDownloadTask> tasks = new ArrayList<>(files.size());
        for (LibraryFile file : files) {
            if (Files.exists(file.path())) {
                // TODO: verify? if configured?
                continue;
            }

            tasks.add(new LibraryDownloadTask(context, file));
        }

        return tasks;
    }

}
