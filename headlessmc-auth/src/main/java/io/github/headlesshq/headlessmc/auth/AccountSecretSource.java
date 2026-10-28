package io.github.headlesshq.headlessmc.auth;

import io.github.headlesshq.headlessmc.console.output.SecretSource;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Supplies the access tokens of all {@link Account}s the {@link AuthService} holds in memory,
 * so that they are never printed.
 * Accounts that have not been loaded yet are skipped, as their tokens cannot be printed either.
 */
@RequiredArgsConstructor
public class AccountSecretSource implements SecretSource {
    private final AuthService authService;

    @Override
    public Collection<String> getSecrets() {
        List<Account> accounts = authService.getLoadedAccounts();
        return accounts.stream().map(Account::getToken).collect(Collectors.toList());
    }

}
