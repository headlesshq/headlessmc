package io.github.headlesshq.headlessmc.auth;

import java.util.Optional;

public interface LastUsedAccountService {
    Optional<Account> getLastUsedAccount() throws AuthException;

    void setLastUsedAccount(Account account) throws AuthException;

}
