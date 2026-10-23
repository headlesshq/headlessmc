package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.config.Holder;
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
     * The path the API is served under by deployments based on
     * <a href="https://github.com/bs-community/blessing-skin-server">Blessing Skin</a>,
     * such as <a href="https://littleskin.cn">LittleSkin</a>.
     *
     * <p>This is not part of the Yggdrasil specification, which only defines the
     * endpoints relative to the API root, so it is configurable via
     * {@code hmc.auth.yggdrasil.api-path}.</p>
     */
    public static final String DEFAULT_API_PATH = "/api/yggdrasil";

    /**
     * The path of the login endpoints, relative to the API root.
     *
     * <p>Like {@link #DEFAULT_API_PATH} this is configurable rather than baked into
     * the {@code @Path} annotations of {@link YggdrasilAPI}, as the specification
     * only defines them relative to the API root.</p>
     */
    public static final String DEFAULT_LOGIN_PATH = "/authserver";

    /**
     * The server suggested to the user when no other url has been configured.
     */
    public static final String DEFAULT_SERVER = "https://littleskin.cn";

    private final Holder<YggdrasilConfig> config;

    public YggdrasilAPI create(String serverUrl) {
        return RestClientBuilder.newBuilder()
            .baseUri(toApiUri(serverUrl, config.get().apiPath(), config.get().loginPath()))
            .build(YggdrasilAPI.class);
    }

    /**
     * Turns a server url into the base uri of the API by appending the given
     * {@code apiPath} and {@code loginPath}.
     *
     * <p>Users may enter either the server root or the full api url, so the api
     * path is not appended if the url already ends with it.</p>
     *
     * @param serverUrl the url of the server, either its root or its api.
     * @param apiPath   the path of the api, relative to the server root.
     * @param loginPath the path of the login endpoints, relative to the api root.
     * @return the base uri of the login endpoints.
     */
    static URI toApiUri(String serverUrl, String apiPath, String loginPath) {
        String url = stripTrailingSlashes(serverUrl);
        if (url.isEmpty()) {
            throw new IllegalArgumentException("The Yggdrasil server url must not be empty.");
        }

        url = appendPath(url, normalizePath(apiPath));
        url = appendPath(url, normalizePath(loginPath));
        return UriBuilder.fromUri(url).build();
    }

    private static String appendPath(String url, String path) {
        if (path.isEmpty() || endsWithPath(url, path)) {
            return url;
        }

        return url + path;
    }

    private static String stripTrailingSlashes(String value) {
        String url = value == null ? "" : value.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    /**
     * Makes sure the path starts with a slash and does not end with one, so that
     * it can be appended to a url without producing double slashes.
     */
    private static String normalizePath(String apiPath) {
        String path = apiPath == null ? "" : apiPath.trim();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.isEmpty()) {
            return "";
        }

        return path.startsWith("/") ? path : "/" + path;
    }

    private static boolean endsWithPath(String url, String path) {
        return url.equals(path) || url.endsWith(path)
            || url.endsWith(path + "/");
    }

}
