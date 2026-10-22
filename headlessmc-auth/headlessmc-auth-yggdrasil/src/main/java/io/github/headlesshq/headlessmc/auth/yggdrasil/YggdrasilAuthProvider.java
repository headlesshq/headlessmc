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

        // the session may still be valid, in which case there is nothing to do
        boolean valid;
        try {
            api.validate(new YggdrasilSession.Validate(stored.getAccessToken(), stored.getClientToken()));
            valid = true;
        } catch (ProcessingException | ApiException e) {
            // validation failed, so the session needs to be refreshed
            valid = false;
        }

        if (valid) {
            return account;
        }

        YggdrasilSession.Response response;
        try {
            response = api.refresh(
                new YggdrasilSession.Refresh(stored.getAccessToken(), stored.getClientToken()));
        } catch (ProcessingException | ApiException e) {
            // the refresh token expired, fall back to a full login if we still have the password
            if (!hasCredentials(stored)) {
                throw new AuthException("Failed to refresh the Yggdrasil session of '"
                    + account.getName() + "'.", e);
            }

            return reauthenticate(stored);
        }

        return store(stored, response);
    }

    private Account reauthenticate(YggdrasilAccount stored) throws AuthException {
        try {
            YggdrasilSession.Response response = clientFactory
                .create(stored.getServerUrl())
                .authenticate(new YggdrasilSession.Authenticate(stored.getUsername(), stored.getPassword()));
            return store(stored, response);
        } catch (ProcessingException | ApiException e) {
            throw new AuthException("Failed to authenticate the Yggdrasil account '"
                + stored.getName() + "'.", e);
        }
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
            profile.getName(),
            stored.getUsername(),
            stored.getPassword()
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

    private static boolean hasCredentials(YggdrasilAccount account) {
        return account.getUsername() != null && account.getPassword() != null;
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
