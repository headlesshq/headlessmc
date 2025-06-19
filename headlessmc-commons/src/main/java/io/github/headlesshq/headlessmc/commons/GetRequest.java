package io.github.headlesshq.headlessmc.commons;

import io.github.headlesshq.headlessmc.progressbar.ProgressBarProvider;
import lombok.Builder;
import lombok.Data;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@Data
@Builder
public class GetRequest {
    private final URI url;
    private final HttpClient httpClient;
    private final ChecksumService checksumService;
    private final @Nullable String sha1;
    private final @Nullable Long size;
    private final @Nullable String progressBarTitle;
    private final @Nullable ProgressBarProvider progressBarProvider;
    @Builder.Default
    private final boolean checkStatusCode = true;
    @Builder.Default
    private final boolean largeFile = false;

    public void toFile(Path path) throws IOException {
        // TODO
    }

    public HttpResponse get() throws IOException {
        if (largeFile) {

        }

        return null;
    }

    public HttpResponse get(IOConsumer<HttpResponse> consumer) throws IOException {
        return null;
    }

}
