package io.github.headlesshq.headlessmc.auth;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AnyAuthProvider implements AuthProvider {
    private final AuthService authService;

    @Override
    public Account refresh(Account account) throws AuthException {
        return authService.refresh(account);
    }

    @Override
    public Optional<Account> getAccount(String name) throws AuthException {
        List<Account> result = new ArrayList<>();
        for (AuthProvider provider : authService.getProviders()) {
            provider.getAccount(name).ifPresent(result::add);
        }

        if (result.size() == 1) {
            return Optional.of(result.getFirst());
        } else if (result.isEmpty()) {
            return Optional.empty();
        } else {
            StringBuilder message = new StringBuilder("Multiple accounts have the name ").append(name).append(":");
            for (Account account : result) {
                message.append("\n-provider: ")
                    .append(account.getProvider())
                    .append(", uuid: ")
                    .append(account.getUuid());
            }

            throw new AuthException(message.toString());
        }
    }

    @Override
    public List<Account> getAccounts() throws AuthException {
        return authService.getProviders().stream()
            .map(AuthProvider::getAccounts)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public void removeAccount(Account account) {
        authService.getProvider(account.getProvider())
            .orElseThrow(() -> new AuthException(
                "Failed to find provider " + account.getProvider() + " for account " + account.getName())
            ).removeAccount(account);
    }

    @Override
    public Authenticator getAuthenticator() {
        return authService.getDefaultProvider().getAuthenticator();
    }

    @Override
    public String getName() {
        return "any";
    }

    @Override
    public long getVersion() {
        return 0;
    }

}
