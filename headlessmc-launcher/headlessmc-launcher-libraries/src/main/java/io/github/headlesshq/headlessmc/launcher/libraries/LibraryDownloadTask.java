package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.RequestException;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadRetry;
import io.github.headlesshq.headlessmc.net.parallel.ParallelTask;
import io.github.headlesshq.headlessmc.net.parallel.UnrecoverableException;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;

@Slf4j
final class LibraryDownloadTask implements ParallelTask {
    private final DownloadContext context;
    private final LibraryFile file;
    private final @Nullable String sha1;
    private final @Nullable Long size;
    private final @Nullable URI url;

    public LibraryDownloadTask(DownloadContext context, LibraryFile file) {
        this.context = context;
        this.file = file;
        Version.Download download = file.download();
        this.sha1 = download == null ? null : download.getSha1();
        this.size = download == null ? null : download.getSize();
        String urlString = download == null ? null : download.getUrl();
        this.url = urlString == null ? null : URI.create(urlString);
    }

    @Override
    public void run() throws HeadlessMcException {
        if (url == null) {
            throw new UnrecoverableException("Failed to find URL for library download " + file);
        }

        download(url);
    }

    public void retry(List<MavenRepository> repositories) throws HeadlessMcException {
        String path = url != null ? url.getPath() : null;
        // TODO: this does not work yet, because natives have other names, use path of URL!!!!
        Artifact artifact = file.library().getArtifact();
        for (MavenRepository repository : repositories) {
            if (url != null && url.toString().startsWith(repository.getUrl().toString())) {
                log.info("Skipping repository {} for library {}", repository, file);
                continue;
            }

            URI url = repository.getJar(artifact);
            log.info("Downloading library {} from {}", artifact, url);
            try {
                download(url);
                return;
            } catch (HeadlessMcException e) {
                log.error("Failed to download library {} from url {}", artifact, url, e);
            }
        }

        throw new RequestException("Failed to download library " + file);
    }

    private void download(URI url) throws HeadlessMcException {
        context.download(url)
            .retry(DownloadRetry.none()) // handled by ParallelTaskService
            .sha1(sha1)
            .size(size)
            .toFile(file.path());
    }

    @Override
    public long getSize() {
        return size == null ? 0L : size;
    }

}
