package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.commons.ChecksumService;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.commons.HttpClientFactory;
import io.github.headlesshq.headlessmc.commons.IOUtil;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;
import net.lenni0451.commons.httpclient.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Default
@Priority(1)
@Specializes
@Alternative
@ApplicationScoped
public class TestDownloadService extends DownloadService {
    @Inject
    public TestDownloadService(HttpClientFactory httpClientFactory, ChecksumService checksumService) {
        super(httpClientFactory, checksumService);
    }

    @Override
    public HttpResponse download(URI from) throws IOException {
        if (from.equals(VanillaVersionManagerImpl.URL)) {
            try (InputStream is = VanillaVersionManagerTest.class.getClassLoader().getResourceAsStream("versions.json")) {
                assertNotNull(is);
                byte[] bytes = IOUtil.toBytes(is);
                return new HttpResponse(from.toURL(), 200, bytes, new HashMap<>());
            }
        }

        return super.download(from);
    }
}
