package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.auth.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// TODO: https://github.com/xiaofanforfabric/headlessmc/blob/main/headlessmc-auth/src/main/java/io/github/headlesshq/headlessmc/auth/YggdrasilClient.java
@Getter
@RequiredArgsConstructor
public class YggdrasilAuthProvider implements AuthProvider {
    public static final String NAME = "yggdrasil";
    public static final String TYPE = "legacy";
    public static final long VERSION = 0L;

    private final AuthStore<YggdrasilAccount> authStore;
    private final String name;
    private final long version;

    @Override
    public Account refresh(Account account) throws AuthException {
        Optional<YggdrasilAccount> yggdrasilAccount = authStore.getById(account.getName());
        // TODO:
        return null;
    }

    @Override
    public Optional<Account> getAccount(String name) throws AuthException {
        return authStore.getById(name).map(YggdrasilAccount::asAccount);
    }

    @Override
    public List<Account> getAccounts() throws AuthException {
        return authStore.stream().map(YggdrasilAccount::asAccount).collect(Collectors.toList());
    }

    @Override
    public void removeAccount(Account account) {
        authStore.remove(account.getName());
    }

    @Override
    public Authenticator getAuthenticator() {
        return new YggdrasilAuthenticator(this);
    }

}
