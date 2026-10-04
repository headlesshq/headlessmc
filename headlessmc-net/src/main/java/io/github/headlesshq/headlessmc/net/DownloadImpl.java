package io.github.headlesshq.headlessmc.net;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Default implementation of {@link Download}.
 */
@Getter
@RequiredArgsConstructor
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "ClassCanBeRecord"})
public class DownloadImpl implements Download {
    private final Optional<Long> potentialSize;
    private final int statusCode;
    private final InputStream inputStream;

    @Override
    public String getAsString(Charset charset) throws IOException {
        return new String(getInputStream().readAllBytes(), charset);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

}
