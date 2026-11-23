package io.github.headlesshq.headlessmc.auth;

import java.util.List;
import java.util.Optional;

public interface AuthProvider {
    String OFFLINE = "offline";
    String DEFAULT = "default";
    String YGGDRASIL = "yggdrasil";

    /**
     * Refreshes the session of the given account, even if it is still valid.
     *
     * @param account the account to refresh.
     * @return the refreshed account.
     * @throws AuthException if the account could not be refreshed.
     */
    Account refresh(Account account) throws AuthException;

    /**
     * Makes sure that the given account can be used, e.g. to launch the game.
     * In contrast to {@link #refresh(Account)} a provider may skip the refresh
     * if it knows that the session of the account is going to be valid for long enough.
     *
     * @param account the account to refresh.
     * @return the account, refreshed if that was necessary.
     * @throws AuthException if the account could not be refreshed.
     */
    default Account refreshIfNeeded(Account account) throws AuthException {
        return refresh(account);
    }

    Optional<Account> getAccount(String name) throws AuthException;

    List<Account> getAccounts() throws AuthException;

    /**
     * Returns the accounts this provider currently holds in memory, without reading them from storage
     * or contacting any service, so it is cheap and cannot fail.
     * Accounts that have not been read yet are missing.
     *
     * @return the accounts currently held in memory.
     */
    List<Account> getLoadedAccounts();

    void removeAccount(Account account);

    Authenticator getAuthenticator();

    String getName();

    long getVersion();

    /**
     * Whether this provider is currently usable.
     * Disabled providers are not exposed by the {@link AuthService}, e.g. because
     * they require configuration that has not been set.
     *
     * @return {@code true} by default.
     */
    default boolean isEnabled() {
        return true;
    }

}
