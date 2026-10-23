package io.github.headlesshq.headlessmc.auth;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final List<AuthProvider> providers;
    private final AuthProvider defaultProvider;

    /**
     * The providers that are currently usable.
     *
     * <p>This is computed on every access instead of once at startup, because
     * {@link AuthProvider#isEnabled()} may change at runtime: providers that need
     * configuration, such as the Yggdrasil one, become enabled as soon as the user
     * sets the required property via the config command.</p>
     */
    private List<AuthProvider> enabledProviders() {
        return providers.stream().filter(AuthProvider::isEnabled).collect(Collectors.toList());
    }

    @Override
    public Account refresh(Account account) {
        return getProvider(account.getProvider())
            .map(provider -> provider.refresh(account))
            .orElseThrow(() -> new AuthException(String.format(
                "Failed to find provider %s, available: %s",
                account.getProvider(),
                enabledProviders().stream()
                    .map(AuthProvider::getName)
                    .collect(Collectors.joining(","))
            )));
    }

    @Override
    public List<Account> getAccounts() {
        return enabledProviders().stream()
            .map(AuthProvider::getAccounts)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<AuthProvider> getProvider(String name) {
        return enabledProviders().stream()
            .filter(provider -> provider.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    public List<AuthProvider> getProviders() {
        return enabledProviders();
    }

    @Override
    public AuthProvider getDefaultProvider() {
        return defaultProvider;
    }

}
