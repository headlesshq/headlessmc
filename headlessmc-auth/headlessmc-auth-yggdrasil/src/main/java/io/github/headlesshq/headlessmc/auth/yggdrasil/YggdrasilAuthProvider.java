package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthException;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthStore;
import io.github.headlesshq.headlessmc.auth.AuthStoreException;
import io.github.headlesshq.headlessmc.auth.Authenticator;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import jakarta.ws.rs.ProcessingException;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Provider for third party Yggdrasil authentication servers.
 *
 * <p>This provider is disabled unless {@code hmc.auth.yggdrasil.url} is set, because
 * HeadlessMc should not encourage playing without owning the game. Accounts added via
 * this provider are therefore treated like offline accounts when launching.</p>
 */
@Getter
public class YggdrasilAuthProvider implements AuthProvider {
    public static final String NAME = AuthProvider.YGGDRASIL;
    public static final String TYPE = Account.TYPE_LEGACY;
    public static final long VERSION = 0L;

    private final AuthStore<YggdrasilAccount> authStore;
    private final YggdrasilClientFactory clientFactory;
    private final Holder<YggdrasilConfig> config;
    private final Consumer<Throwable> exceptionLogger;
    private final String name;
    private final long version;

    public YggdrasilAuthProvider(
        AuthStore<YggdrasilAccount> authStore,
        YggdrasilClientFactory clientFactory,
        Holder<YggdrasilConfig> config,
        Consumer<Throwable> exceptionLogger,
        String name,
        long version) {
        this.authStore = authStore;
        this.clientFactory = clientFactory;
        this.config = config;
        this.exceptionLogger = exceptionLogger;
        this.name = name;
        this.version = version;
    }

    /**
     * Whether this provider is enabled, which is only the case when a server url
     * has been configured, see {@link YggdrasilConfig#url()}.
     */
    @Override
    public boolean isEnabled() {
        Optional<String> url = config.get().url();
        return url.isPresent() && !url.get().trim().isEmpty();
    }

    @Override
    public Account refresh(Account account) throws AuthException {
        YggdrasilAccount stored = storedAccount(account.getName());
        if (stored == null) {
            throw new AuthException("Cannot refresh the Yggdrasil account '"
                + account.getName() + "', it is not stored.");
        }

        if (stored.getServerUrl() == null) {
            throw new AuthException("Cannot refresh the Yggdrasil account '"
                + account.getName() + "', the server url is unknown.");
        }

        YggdrasilAPI api = clientFactory.create(stored.getServerUrl());

        // The session may still be valid, in which case there is nothing to do.
        // Only a rejected token means it has to be refreshed, a connection problem
        // must not be confused with an expired session.
        if (isValid(api, stored)) {
            return account;
        }

        YggdrasilSession.Response response;
        try {
            response = api.refresh(
                new YggdrasilSession.Refresh(stored.getAccessToken(), stored.getClientToken()));
        } catch (ProcessingException e) {
            throw new AuthException("Could not reach the Yggdrasil server " + stored.getServerUrl()
                + " to refresh the session of '" + account.getName() + "'.", e);
        } catch (ApiException e) {
            exceptionLogger.accept(new AuthException(expiredMessage(account), e));
            return withoutToken(account);
        }

        return store(stored, response);
    }

    /**
     * Drops the token, so that launching continues as an offline launch.
     * The user has been warned via {@link #expiredMessage(Account)}.
     */
    static Account withoutToken(Account account) {
        return new Account(
            account.getProvider(),
            account.getName(),
            account.getUuid(),
            "",
            account.getType(),
            account.getXuid()
        );
    }

    /**
     * Whether the stored token is still accepted by the server.
     *
     * @return {@code true} if the token is valid.
     * @throws AuthException if the server could not be reached, as that is not a
     *                       statement about the token.
     */
    private static boolean isValid(YggdrasilAPI api, YggdrasilAccount stored) throws AuthException {
        try {
            api.validate(new YggdrasilSession.Validate(
                stored.getAccessToken(), stored.getClientToken()));
            return true;
        } catch (ProcessingException e) {
            throw new AuthException("Could not reach the Yggdrasil server "
                + stored.getServerUrl() + " to validate the session of '"
                + stored.getName() + "'.", e);
        } catch (ApiException e) {
            return false;
        }
    }

    /**
     * The message shown when the stored session cannot be refreshed anymore.
     *
     * <p>Without a valid token the game does not authenticate against the third
     * party server, so launching would silently behave like an offline launch.
     * That is confusing, which is why this is spelled out.</p>
     */
    static String expiredMessage(Account account) {
        return "The Yggdrasil session of '" + account.getName() + "' has expired! "
            + "Launching now would behave like an offline launch, so the server will "
            + "most likely reject you with 'invalid session'. Please log in again.";
    }

    /**
     * Persists the refreshed session and returns the matching {@link Account}.
     */
    private Account store(YggdrasilAccount stored, YggdrasilSession.Response response) throws AuthException {
        YggdrasilSession.Profile profile = response.getSelectedProfile();
        if (profile == null) {
            // The server did not bind a profile, so the token cannot be used to join
            // a server. This happens when the account owns multiple profiles, which is
            // why we always keep the profile the account was created with.
            profile = new YggdrasilSession.Profile(stored.getUuid(), stored.getName());
        }

        YggdrasilAccount refreshed = new YggdrasilAccount(
            stored.getServerUrl(),
            response.getAccessToken(),
            response.getClientToken(),
            profile.getId(),
            profile.getName()
        );

        try {
            // the name may change if the selected profile changed
            if (!refreshed.getName().equalsIgnoreCase(stored.getName())) {
                authStore.remove(stored.getName());
            }
            authStore.add(refreshed.getName(), refreshed);
        } catch (AuthStoreException e) {
            throw new AuthException("Failed to store the refreshed Yggdrasil account '"
                + refreshed.getName() + "'.", e);
        }

        return refreshed.asAccount();
    }

    private YggdrasilAccount storedAccount(String name) {
        try {
            return authStore.getById(name).orElse(null);
        } catch (AuthStoreException e) {
            exceptionLogger.accept(e);
            return null;
        }
    }

    @Override
    public Optional<Account> getAccount(String name) {
        return Optional.ofNullable(storedAccount(name)).map(YggdrasilAccount::asAccount);
    }

    @Override
    public List<Account> getAccounts() {
        try {
            return authStore.stream().map(YggdrasilAccount::asAccount).collect(Collectors.toList());
        } catch (AuthStoreException e) {
            exceptionLogger.accept(e);
            return List.of();
        }
    }

    @Override
    public void removeAccount(Account account) {
        try {
            authStore.remove(account.getName());
        } catch (AuthStoreException e) {
            exceptionLogger.accept(e);
        }
    }

    @Override
    public Authenticator getAuthenticator() {
        return new YggdrasilAuthenticator(this);
    }

}
