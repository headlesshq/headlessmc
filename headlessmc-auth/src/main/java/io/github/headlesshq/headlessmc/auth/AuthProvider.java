package io.github.headlesshq.headlessmc.auth;

import java.util.List;
import java.util.Optional;

public interface AuthProvider {
    String OFFLINE = "offline";
    String DEFAULT = "default";

    Account refresh(Account account) throws AuthException;

    Optional<Account> getAccount(String name) throws AuthException;

    List<Account> getAccounts() throws AuthException;

    void removeAccount(Account account);

    Authenticator getAuthenticator();

    String getName();

    long getVersion();

}
