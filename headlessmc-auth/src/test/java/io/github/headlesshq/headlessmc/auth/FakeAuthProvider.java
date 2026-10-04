package io.github.headlesshq.headlessmc.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class FakeAuthProvider implements AuthProvider {
    private final String name;
    final List<Account> accounts = new ArrayList<>();
    boolean failOnGetAccount;
    Account refreshed;

    FakeAuthProvider(String name, Account... accounts) {
        this.name = name;
        this.accounts.addAll(List.of(accounts));
    }

    static Account account(String provider, String name) {
        return new Account(provider, name, "uuid-" + name, "token", Account.TYPE_MSA, "xuid");
    }

    @Override
    public Account refresh(Account account) throws AuthException {
        refreshed = account;
        return account;
    }

    @Override
    public Optional<Account> getAccount(String name) throws AuthException {
        if (failOnGetAccount) {
            throw new AuthException("simulated failure in " + this.name);
        }

        return accounts.stream().filter(account -> account.getName().equals(name)).findFirst();
    }

    @Override
    public List<Account> getAccounts() throws AuthException {
        return accounts;
    }

    @Override
    public void removeAccount(Account account) {
        accounts.remove(account);
    }

    @Override
    public Authenticator getAuthenticator() {
        return java.util.Map::of;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getVersion() {
        return 0;
    }

}
