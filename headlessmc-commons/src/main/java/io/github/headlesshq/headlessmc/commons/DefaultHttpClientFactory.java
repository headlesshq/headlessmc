package io.github.headlesshq.headlessmc.commons;

import io.github.headlesshq.headlessmc.api.settings.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.RetryHandler;
import net.lenni0451.commons.httpclient.constants.Headers;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DefaultHttpClientFactory implements HttpClientFactory {
    private final DownloadSettings settings;
    private final Config config;

    @Override
    @Produces
    @Dependent
    public HttpClient create() {
        HttpClient result = createDefaultHttpClient();
        if (config.get(settings.httpUserAgentEnabled())) {
            result.setHeader(Headers.USER_AGENT, config.get(settings.httpUserAgent()));
        }

        return result;
    }

    public HttpClient createDefaultHttpClient() {
        return new HttpClient()
                .setConnectTimeout(5_000)
                .setReadTimeout(5_000 * 2)
                .setCookieManager(null)
                .setFollowRedirects(true)
                .setRetryHandler(new RetryHandler(0, 50));
    }

}
