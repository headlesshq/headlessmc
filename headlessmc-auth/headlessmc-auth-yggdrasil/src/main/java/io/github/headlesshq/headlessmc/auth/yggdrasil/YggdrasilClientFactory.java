package io.github.headlesshq.headlessmc.auth.yggdrasil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;

/**
 * Builds {@link YggdrasilAPI} instances for a given server url.
 *
 * <p>Every third party Yggdrasil server has its own url, and users may enter it at
 * login time, so the base uri cannot be baked into {@code @RegisterRestClient}.</p>
 */
@ApplicationScoped
@RequiredArgsConstructor
public class YggdrasilClientFactory {
    /**
     * The url every Yggdrasil server implements the API under.
     */
    static final String API_PATH = "/api/yggdrasil";

    /**
     * The server suggested to the user when no other url has been configured.
     */
    public static final String DEFAULT_SERVER = "https://littleskin.cn";

    public YggdrasilAPI create(String serverUrl) {
        return RestClientBuilder.newBuilder()
            .baseUri(toApiUri(serverUrl))
            .build(YggdrasilAPI.class);
    }

    /**
     * Turns a server url into the base uri of the API, appending {@link #API_PATH}
     * if the url points at the server root instead of the API itself.
     * Trailing slashes are removed so that the {@code @Path} values do not produce
     * double slashes.
     */
    static URI toApiUri(String serverUrl) {
        String url = serverUrl == null ? "" : serverUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        if (url.isEmpty()) {
            throw new IllegalArgumentException("The Yggdrasil server url must not be empty.");
        }

        // users may enter either the server root or the full api url
        if (!url.endsWith(API_PATH)) {
            url = url + API_PATH;
        }

        return UriBuilder.fromUri(url).build();
    }

}
