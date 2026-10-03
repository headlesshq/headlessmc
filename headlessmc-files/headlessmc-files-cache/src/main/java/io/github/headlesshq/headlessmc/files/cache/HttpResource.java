package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.net.DownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadService;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import java.net.URI;
import java.util.Optional;

@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public final class HttpResource<V> implements CacheSource<V> {
    private final CacheExceptionHandler exceptionHandler;
    private final DownloadService downloadService;
    private final Downloader<V> download;
    @ToString.Include
    private final URI url;

    @Override
    public Optional<V> get() throws HeadlessMcException {
        try {
            V value = download.download(
                downloadService
                    .download(url)
                    //temporary workout around meta.fabric
                    //somehow the first attempt without cookies fails
                    .retry(3, 1000, 2.0, 0.5)
            );
            return Optional.of(value);
        } catch (UncheckedInterruptedException e) {
            throw e;
        } catch (HeadlessMcException e) {
            exceptionHandler.error(this, "Failed to fetch url " + url, e);
        }

        return Optional.empty();
    }

    @FunctionalInterface
    public interface Downloader<V> {
        V download(DownloadBuilder downloadBuilder) throws HeadlessMcException;
    }

}
