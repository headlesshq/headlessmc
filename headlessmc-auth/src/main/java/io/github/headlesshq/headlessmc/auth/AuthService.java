package io.github.headlesshq.headlessmc.auth;

import java.util.List;
import java.util.Optional;

public interface AuthService {
    Account refresh(Account account);

    List<Account> getAccounts();

    /**
     * @return the {@link AuthProvider#getLoadedAccounts()} of all providers, including disabled ones.
     */
    List<Account> getLoadedAccounts();

    Optional<AuthProvider> getProvider(String name);

    List<AuthProvider> getProviders();

    AuthProvider getDefaultProvider();

}
