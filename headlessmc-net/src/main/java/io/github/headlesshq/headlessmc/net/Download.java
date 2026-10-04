package io.github.headlesshq.headlessmc.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

public interface Download extends Closeable {
    Optional<Long> getPotentialSize() throws IOException;

    int getStatusCode() throws IOException;

    String getAsString(Charset charset) throws IOException;

    InputStream getInputStream() throws IOException;

}
