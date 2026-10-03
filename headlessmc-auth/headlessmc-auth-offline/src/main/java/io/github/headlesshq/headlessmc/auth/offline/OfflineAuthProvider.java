package io.github.headlesshq.headlessmc.auth.offline;

import io.github.headlesshq.headlessmc.auth.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class OfflineAuthProvider implements AuthProvider {
    public static final long VERSION = Account.SCHEMA_VERSION;

    private final AuthStore<Account> authStore;
    private final Consumer<Throwable> exceptionLogger;
    private final String name;
    private final long version;

    @Override
    public Account refresh(Account account) throws AuthException {
        // use generics to make this type safe?
        if (!getName().equals(account.getProvider())) {
            throw new IllegalArgumentException(
                "Cannot refresh account for provider " + account.getProvider() + " with provider " + getName()
            );
        }

        return account;
    }

    @Override
    public Optional<Account> getAccount(String name) throws AuthException {
        try {
            return authStore.getById(name);
        } catch (AuthStoreException e) {
            exceptionLogger.accept(e);
            return Optional.empty();
        }
    }

    @Override
    public List<Account> getAccounts() throws AuthException {
        try {
            return authStore.stream().collect(Collectors.toList());
        } catch (AuthStoreException e) {
            exceptionLogger.accept(e);
            return new ArrayList<>();
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
        return new OfflineAuthenticator(this);
    }

}
