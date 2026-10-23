package io.github.headlesshq.headlessmc.auth.yggdrasil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.AuthStoreException;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.auth.Credentials;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.Password;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Unmodifiable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticator for third party Yggdrasil servers.
 *
 * <p>Accounts logged in this way are treated like offline accounts with regards to
 * launching: headless launches are always allowed, but a launch with a GUI requires
 * an account of the {@code default} provider, see
 * {@code AbstractClientAndServerLaunchCommand}.</p>
 */
@RequiredArgsConstructor
final class YggdrasilAuthenticator implements Authenticator {
    private final YggdrasilAuthProvider provider;

    @Override
    public @Unmodifiable Map<String, Method<Console>> getMethods() {
        Map<String, Method<Console>> result = new LinkedHashMap<>();
        result.put(Authenticator.DEFAULT_METHOD, loginWithUserInput());
        return result;
    }

    /**
     * Logs in interactively, asking for the server url, username and password.
     */
    private Method<Console> loginWithUserInput() {
        return console -> {
            String serverUrl = console.read(
                "Authentication server url (default: " + YggdrasilClientFactory.DEFAULT_SERVER + "):");
            if (serverUrl.trim().isEmpty()) {
                serverUrl = YggdrasilClientFactory.DEFAULT_SERVER;
            }

            String username = console.read("Username:");
            String password;
            try (Password pwd = console.readPassword("Password:")) {
                password = new String(pwd.get());
            }

            return login(serverUrl, username, password, console);
        };
    }

    @Override
    public Optional<Method<Credentials>> loginWithCredentials() {
        return Optional.of(credentials -> login(
            configuredOrDefaultServer(),
            credentials.getEmail(),
            credentials.getPassword(),
            // there is no console to ask for a profile, use the selected one
            null
        ));
    }

    /**
     * The configured server url, or the default one if none has been set.
     * Logging in without a console means there is no chance to ask for a url.
     */
    private String configuredOrDefaultServer() {
        return provider.getConfig()
            .get()
            .url()
            .orElse(YggdrasilClientFactory.DEFAULT_SERVER);
    }

    /**
     * Authenticates against the given server and stores the resulting account.
     *
     * @param console used to let the user pick a profile if the account has multiple,
     *                may be {@code null} in which case the profile selected by the
     *                server is used.
     */
    private Account login(
        String serverUrl, String username, String password, Console console) throws AuthException {
        YggdrasilAPI api = provider.getClientFactory().create(serverUrl);

        YggdrasilSession.Response response = authenticate(api, serverUrl, username, password);
        YggdrasilSession.Profile profile = selectProfile(response, console);

        // Accounts with multiple profiles get a token without a bound profile.
        // Such a token cannot be used to join a server, so we have to refresh it while
        // selecting the profile, which issues a new token bound to that profile.
        if (response.getSelectedProfile() == null) {
            response = selectProfileOnServer(api, response, profile);
        }

        YggdrasilAccount account = new YggdrasilAccount(
            serverUrl,
            response.getAccessToken(),
            response.getClientToken(),
            profile.getId(),
            profile.getName()
        );

        try {
            provider.getAuthStore().add(account.getName(), account);
        } catch (AuthStoreException e) {
            throw new AuthException("Failed to store the Yggdrasil account '"
                + account.getName() + "'.", e);
        }

        printAgentHint(serverUrl, console);
        return account.asAccount();
    }

    /**
     * Tells the user how to join servers that require the third party authentication.
     *
     * <p>Login alone is not enough: the game only talks to a third party auth server
     * when authlib-injector is injected as a java agent. Since HeadlessMc deliberately
     * does not handle the agent itself, we show the vm args the user has to add to
     * their profile.</p>
     *
     * <p>The prefetched metadata is deliberately <b>not</b> printed here: it is a long
     * blob that is easy to mistake for a credential, and people do share their
     * terminal. It is optional anyway, authlib-injector fetches the metadata itself.</p>
     */
    private void printAgentHint(String serverUrl, Console console) {
        if (console == null) {
            return;
        }

        printLine(console, "");
        printLine(console, "This account uses third party Yggdrasil authentication. "
            + "To join servers with online-mode enabled, authlib-injector is required.");
        printLine(console, "Add it to the vm args of the profile you use for this account:");
        printLine(console, "  profile edit <profile> vm-args "
            + "\"-javaagent:/path/to/authlib-injector.jar=" + serverUrl + "\"");
    }

    private static void printLine(Console console, String message) {
        console.write(message, System.lineSeparator());
    }

    private YggdrasilSession.Response authenticate(
        YggdrasilAPI api, String serverUrl, String username, String password) throws AuthException {
        try {
            return api.authenticate(new YggdrasilSession.Authenticate(username, password));
        } catch (ApiException e) {
            throw new AuthException("Failed to authenticate with " + serverUrl
                + " (" + describe(e) + ").", e);
        } catch (ProcessingException | IllegalArgumentException e) {
            throw new AuthException("Failed to reach the Yggdrasil server " + serverUrl + ".", e);
        }
    }

    /**
     * Binds the current token to the given profile, as described in the Yggdrasil spec:
     * "If the request contains selectedProfile, this is a profile selection operation.
     * It requires that the profile the original token is bound to is empty, and the new
     * token will be bound to the profile specified by selectedProfile."
     */
    private YggdrasilSession.Response selectProfileOnServer(
        YggdrasilAPI api, YggdrasilSession.Response response, YggdrasilSession.Profile profile)
        throws AuthException {
        try {
            return api.refresh(new YggdrasilSession.Refresh(
                response.getAccessToken(), response.getClientToken(), profile));
        } catch (ApiException e) {
            throw new AuthException("Failed to select the profile '" + profile.getName()
                + "' (" + describe(e) + ").", e);
        } catch (ProcessingException e) {
            throw new AuthException("Failed to select the profile '" + profile.getName() + "'.", e);
        }
    }

    /**
     * Turns the error body of a failed request into a readable message.
     * The Yggdrasil spec requires an {@code errorMessage} field for business errors.
     */
    static String describe(ApiException e) {
        Response httpResponse = e.getResponse();
        if (httpResponse == null) {
            return e.getMessage();
        }

        String body;
        try {
            body = httpResponse.readEntity(String.class);
        } catch (RuntimeException readFailure) {
            return e.getMessage();
        }

        if (body == null || body.isEmpty()) {
            return e.getMessage();
        }

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("errorMessage")) {
                return json.get("errorMessage").getAsString();
            }
        } catch (RuntimeException notJson) {
            // fall through and return the raw body
        }

        return body;
    }

    /**
     * Determines the profile to bind the token to.
     *
     * <p>If the server already bound a profile we use it. Otherwise the account has
     * multiple profiles and the user has to choose one.</p>
     */
    private static YggdrasilSession.Profile selectProfile(YggdrasilSession.Response response, Console console)
        throws AuthException {
        YggdrasilSession.Profile bound = response.getSelectedProfile();
        if (bound != null) {
            return bound;
        }

        List<YggdrasilSession.Profile> profiles = response.getAvailableProfiles();
        if (profiles == null || profiles.isEmpty()) {
            throw new AuthException("The Yggdrasil server did not return any profile for this account.");
        }

        if (profiles.size() == 1) {
            return profiles.get(0);
        }

        if (console == null) {
            throw new AuthException("The account owns " + profiles.size()
                + " profiles, a profile has to be selected but no console is available.");
        }

        console.write("Found " + profiles.size() + " profiles:", "");
        for (int i = 0; i < profiles.size(); i++) {
            console.write("  " + (i + 1) + ". " + profiles.get(i).getName(), "");
        }

        String input = console.read("Select a profile (1-" + profiles.size() + "):");
        try {
            int index = Integer.parseInt(input.trim());
            if (index >= 1 && index <= profiles.size()) {
                return profiles.get(index - 1);
            }
        } catch (NumberFormatException e) {
            // fall through to the error below
        }

        throw new AuthException("Invalid profile selection: '" + input + "'.");
    }

}
