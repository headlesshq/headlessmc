package io.github.headlesshq.headlessmc.auth.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.AuthStore;
import io.github.headlesshq.headlessmc.auth.InMemoryAuthStore;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.exceptions.HttpRequestException;
import net.lenni0451.commons.httpclient.executor.RequestExecutor;
import net.lenni0451.commons.httpclient.requests.HttpContentRequest;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McAuthProviderTest {
    private static final String PROVIDER = "default";
    private static final String NAME = "Steve";
    private static final String UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5";

    private static final String LOGIN = "POST https://api.minecraftservices.com/launcher/login";
    private static final String PROFILE = "GET https://api.minecraftservices.com/minecraft/profile";
    private static final String SISU = "POST https://sisu.xboxlive.com/authorize";

    private static final String CACHED_XSTS = "cached-xsts";
    private static final String NEW_XSTS = "new-xsts";
    private static final String CACHED_TOKEN = "cached-minecraft-token";
    private static final String NEW_TOKEN = "new-minecraft-token";

    private final FakeServices services = new FakeServices();
    private final HttpClient httpClient = new HttpClient(services::executor);
    private final AuthStore<JavaAuthManager> store = new InMemoryAuthStore<>();
    private final McAuthProvider provider = new McAuthProvider(httpClient, PROVIDER, store);
    private final Account account = new Account(PROVIDER, NAME, UUID, CACHED_TOKEN, Account.TYPE_MSA, "");

    @Test
    void refreshRetriesWithANewXstsTokenIfTheCachedOneIsRejected() {
        // https://github.com/headlesshq/headlessmc/issues/586
        JavaAuthManager authManager = store(Duration.ofMinutes(40), Duration.ofHours(23));

        Account refreshed = provider.refresh(account);

        assertEquals(NEW_TOKEN, refreshed.getToken());
        assertEquals(NAME, refreshed.getName());
        assertEquals(UUID, refreshed.getUuid());
        assertEquals(List.of(LOGIN, SISU, LOGIN, PROFILE), services.requests);
        assertEquals(List.of(CACHED_XSTS, NEW_XSTS), services.loginXstsTokens);
        assertEquals(NEW_XSTS, authManager.getJavaXstsToken().getCached().getToken());
        assertEquals(NEW_TOKEN, provider.getAccount(NAME).orElseThrow().getToken());
    }

    @Test
    void refreshRetriesOnlyOnce() {
        store(Duration.ofMinutes(40), Duration.ofHours(23));
        services.acceptedXstsToken = "none";

        AuthException e = assertThrows(AuthException.class, () -> provider.refresh(account));

        assertStatus(401, e);
        assertEquals(1, e.getCause().getSuppressed().length);
        assertEquals(List.of(LOGIN, SISU, LOGIN), services.requests);
        assertEquals(CACHED_TOKEN, provider.getAccount(NAME).orElseThrow().getToken());
    }

    @Test
    void refreshDoesNotRetryIfTheXstsTokenHasJustBeenRefreshed() {
        // the XSTS token has expired, so it is refreshed for the first request already
        store(Duration.ofMinutes(-1), Duration.ofHours(23));
        services.acceptedXstsToken = "none";

        AuthException e = assertThrows(AuthException.class, () -> provider.refresh(account));

        assertStatus(401, e);
        assertEquals(List.of(SISU, LOGIN), services.requests);
    }

    @Test
    void refreshDoesNotRetryOtherErrors() {
        store(Duration.ofMinutes(40), Duration.ofHours(23));
        services.loginFailureStatus = 429;

        AuthException e = assertThrows(AuthException.class, () -> provider.refresh(account));

        assertStatus(429, e);
        assertEquals(List.of(LOGIN), services.requests);
    }

    @Test
    void refreshRequestsANewTokenEvenIfTheCachedOneIsValid() {
        store(Duration.ofMinutes(40), Duration.ofHours(23));
        services.acceptedXstsToken = CACHED_XSTS;

        assertEquals(NEW_TOKEN, provider.refresh(account).getToken());
        assertEquals(List.of(LOGIN, PROFILE), services.requests);
    }

    @Test
    void refreshIfNeededKeepsATokenThatIsValidForLongEnough() {
        store(Duration.ofMinutes(40), Duration.ofHours(23));

        Account result = provider.refreshIfNeeded(account);

        assertEquals(CACHED_TOKEN, result.getToken());
        assertEquals(NAME, result.getName());
        assertEquals(List.of(), services.requests);
    }

    @Test
    void refreshIfNeededRefreshesATokenThatIsAboutToExpire() {
        store(Duration.ofMinutes(40), Duration.ofHours(1));
        services.acceptedXstsToken = CACHED_XSTS;

        assertEquals(NEW_TOKEN, provider.refreshIfNeeded(account).getToken());
        assertEquals(List.of(LOGIN, PROFILE), services.requests);
    }

    @Test
    void refreshIfNeededRetriesWithANewXstsTokenIfTheCachedOneIsRejected() {
        store(Duration.ofMinutes(40), Duration.ofMinutes(-1));

        assertEquals(NEW_TOKEN, provider.refreshIfNeeded(account).getToken());
        assertEquals(List.of(LOGIN, SISU, LOGIN, PROFILE), services.requests);
    }

    private static void assertStatus(int status, AuthException e) {
        HttpRequestException cause = assertInstanceOf(HttpRequestException.class, e.getCause());
        assertEquals(status, cause.getResponse().getStatusCode());
    }

    /**
     * Stores an account like it is read from the accounts file, with tokens that expire after the given durations.
     */
    private JavaAuthManager store(Duration xstsValidity, Duration minecraftTokenValidity) {
        MsaToken msaToken = new MsaToken(in(Duration.ofHours(1)), "msa-access-token", "msa-refresh-token");
        JsonObject json = JavaAuthManager.toJson(JavaAuthManager.create(httpClient).login(msaToken));
        json.add("xblDeviceToken", json(
            "{'expireTimeMs': %d, 'token': 'device-token', 'deviceId': 'device-id'}", in(Duration.ofDays(10))));
        json.add("xblUserToken", json(
            "{'expireTimeMs': %d, 'token': 'user-token', 'userHash': 'user-hash'}", in(Duration.ofDays(3))));
        json.add("xblTitleToken", json(
            "{'expireTimeMs': %d, 'token': 'title-token', 'titleId': 'title-id'}", in(Duration.ofDays(10))));
        json.add("javaXstsToken", json(
            "{'expireTimeMs': %d, 'token': '" + CACHED_XSTS + "', 'userHash': 'user-hash'}", in(xstsValidity)));
        json.add("minecraftToken", json(
            "{'expireTimeMs': %d, 'type': 'Bearer', 'token': '" + CACHED_TOKEN + "'}", in(minecraftTokenValidity)));
        json.add("minecraftProfile", json("{'id': '" + UUID + "', 'name': '" + NAME + "'}"));

        JavaAuthManager authManager = JavaAuthManager.fromJson(httpClient, json);
        store.add(NAME, authManager);
        return authManager;
    }

    private static long in(Duration duration) {
        return System.currentTimeMillis() + duration.toMillis();
    }

    private static JsonObject json(String format, Object... args) {
        return JsonParser.parseString(String.format(format, args)).getAsJsonObject();
    }

    /**
     * Answers the requests of a {@link JavaAuthManager} like the Xbox and Minecraft services.
     * By default, the Minecraft services reject the cached XSTS token, although it has not expired.
     */
    private static final class FakeServices {
        final List<String> requests = new ArrayList<>();
        final List<String> loginXstsTokens = new ArrayList<>();
        String acceptedXstsToken = NEW_XSTS;
        int loginFailureStatus = 401;

        RequestExecutor executor(HttpClient client) {
            return new RequestExecutor(client) {
                @Override
                public HttpResponse execute(HttpRequest request) throws IOException {
                    return answer(request);
                }
            };
        }

        private HttpResponse answer(HttpRequest request) throws IOException {
            String key = request.getMethod() + " " + request.getURL();
            requests.add(key);
            switch (key) {
                case LOGIN:
                    return login(request);
                case SISU:
                    String notAfter = Instant.now().plus(Duration.ofHours(16)).toString();
                    return response(request, 200, json(
                        "{'UserToken': {'NotAfter': '%1$s', 'Token': 'new-user-token',"
                            + " 'DisplayClaims': {'xui': [{'uhs': 'user-hash'}]}},"
                            + " 'TitleToken': {'NotAfter': '%1$s', 'Token': 'new-title-token',"
                            + " 'DisplayClaims': {'xti': {'tid': 'title-id'}}},"
                            + " 'AuthorizationToken': {'NotAfter': '%1$s', 'Token': '" + NEW_XSTS + "',"
                            + " 'DisplayClaims': {'xui': [{'uhs': 'user-hash'}]}}}", notAfter));
                case PROFILE:
                    return response(request, 200, json(
                        "{'id': '" + UUID.replace("-", "") + "', 'name': '" + NAME + "'}"));
                default:
                    throw new IOException("Unexpected request " + key);
            }
        }

        private HttpResponse login(HttpRequest request) throws IOException {
            String body = ((HttpContentRequest) request).getContent().getAsString();
            String xstsToken = JsonParser.parseString(body).getAsJsonObject().get("xtoken").getAsString();
            xstsToken = xstsToken.substring(xstsToken.indexOf(';') + 1);
            loginXstsTokens.add(xstsToken);
            if (!acceptedXstsToken.equals(xstsToken)) {
                // the response of the issue
                return response(request, loginFailureStatus, json(
                    "{'path': '/launcher/login', 'error': 'UNAUTHORIZED'}"));
            }

            return response(request, 200, json(
                "{'expires_in': 86400, 'token_type': 'Bearer', 'access_token': '" + NEW_TOKEN + "'}"));
        }

        private static HttpResponse response(HttpRequest request, int status, JsonObject json) {
            return new HttpResponse(
                request.getURL(),
                status,
                json.toString().getBytes(StandardCharsets.UTF_8),
                Map.of("Content-Type", List.of("application/json"))
            );
        }
    }

}
