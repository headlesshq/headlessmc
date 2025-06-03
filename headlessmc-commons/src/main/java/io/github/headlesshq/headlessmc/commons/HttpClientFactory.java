package io.github.headlesshq.headlessmc.commons;

import net.lenni0451.commons.httpclient.HttpClient;

@FunctionalInterface
public interface HttpClientFactory {
    HttpClient create();

}
