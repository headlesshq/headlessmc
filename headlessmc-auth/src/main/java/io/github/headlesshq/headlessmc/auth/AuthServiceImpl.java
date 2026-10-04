package io.github.headlesshq.headlessmc.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final List<AuthProvider> providers;
    private final AuthProvider defaultProvider;

    @Override
    public Account refresh(Account account) {
        return getProvider(account.getProvider())
            .map(provider -> provider.refresh(account))
            .orElseThrow(() -> new AuthException(String.format(
                "Failed to find provider %s, available: %s",
                account.getProvider(),
                providers.stream()
                    .map(AuthProvider::getName)
                    .collect(Collectors.joining(","))
            )));
    }

    @Override
    public List<Account> getAccounts() {
        return providers.stream()
            .map(AuthProvider::getAccounts)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<AuthProvider> getProvider(String name) {
        return providers.stream()
            .filter(provider -> provider.getName().equalsIgnoreCase(name))
            .findFirst();
    }

}
