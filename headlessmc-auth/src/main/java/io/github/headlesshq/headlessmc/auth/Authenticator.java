package io.github.headlesshq.headlessmc.auth;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.Password;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

public interface Authenticator {
    String DEFAULT_METHOD = "default";
    String METHOD_WEBVIEW = "webview";
    String METHOD_CREDENTIALS = "credentials";

    @Unmodifiable
    Map<String, Method<Console>> getMethods();

    /**
     * Represents a login method.
     * E.g. a login using the Microsoft Authentication Code,
     * or via Credentials, or via a Webview.
     *
     * @param <T> the type of argument to the method.
     */
    @FunctionalInterface
    interface Method<T> {
        /**
         * Attempts to log in using this method and the provided argument.
         *
         * @param args the arguments required for this method, e.g. credentials.
         * @return the account that has been logged in to.
         * @throws AuthException if something goes wrong.
         */
        Account login(T args) throws AuthException;
    }

    default Optional<Method<Credentials>> loginWithCredentials() throws AuthException {
        return Optional.empty();
    }

    default Optional<Method<Console>> loginWithInteractiveCredentials() {
        return loginWithCredentials()
            .map(method -> (console) -> {
                String email = console.read("Please enter your email:");
                try (Password password = console.readPassword("Please enter your password:")) {
                    return method.login(new Credentials(email, new String(password.get())));
                } catch (IllegalStateException e) {
                    throw new AuthException("Failed to login with credentials, " +
                        "you may want to check if you have 2FA enabled, " +
                        "as it is currently not supported.", e);
                }
            });
    }

}
