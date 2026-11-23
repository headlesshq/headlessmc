package io.github.headlesshq.headlessmc.auth;

import java.util.List;
import java.util.Optional;

public interface AuthService {
    /**
     * @see AuthProvider#refresh(Account)
     */
    Account refresh(Account account);

    /**
     * @see AuthProvider#refreshIfNeeded(Account)
     */
    Account refreshIfNeeded(Account account);

    List<Account> getAccounts();

    /**
     * @return the {@link AuthProvider#getLoadedAccounts()} of all providers, including disabled ones.
     */
    List<Account> getLoadedAccounts();

    Optional<AuthProvider> getProvider(String name);

    List<AuthProvider> getProviders();

    AuthProvider getDefaultProvider();

}
