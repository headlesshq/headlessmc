package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.commons.DownloadService;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;

@RequiredArgsConstructor
public abstract class AbstractHttpCache<T> {
    private final DownloadService downloadService;
    private final URI url;

    private volatile @Nullable T value;

    protected abstract T parse(HttpResponse response) throws IOException;

    public T get() {
        T result = value;
        if (result == null) {
            result = download();
            this.value = result;
            return result;
        }

        return result;
    }

    private T download() {
        try {
            HttpResponse response = downloadService.download(url);
            return parse(response);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
