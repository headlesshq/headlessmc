package io.github.headlesshq.headlessmc.launcher.backup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Finds all {@link BackupProvider} beans.
 */
@ApplicationScoped
public class BackupProviderService {
    /**
     * The name of the provider used if none is specified.
     */
    public static final String DEFAULT_PROVIDER = "profile";

    private final Supplier<List<BackupProvider>> providers;

    @Inject
    public BackupProviderService(Instance<BackupProvider> providers) {
        this.providers = () -> providers.stream().toList();
    }

    /**
     * @param providers a fixed list of providers.
     */
    public BackupProviderService(List<BackupProvider> providers) {
        List<BackupProvider> copy = List.copyOf(providers);
        this.providers = () -> copy;
    }

    /**
     * @return all providers, sorted by name.
     */
    public List<BackupProvider> getProviders() {
        return providers.get().stream()
            .sorted(Comparator.comparing(BackupProvider::getName))
            .toList();
    }

    /**
     * @param name the name of the provider.
     * @return the provider with the given name.
     */
    public Optional<BackupProvider> findProvider(String name) {
        return getProviders().stream()
            .filter(provider -> provider.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    /**
     * @param name the name of the provider.
     * @return the provider with the given name.
     * @throws IllegalArgumentException if no provider has the given name.
     */
    public BackupProvider getProvider(String name) {
        return findProvider(name).orElseThrow(() -> new IllegalArgumentException(
            "Unknown backup provider '" + name + "', available: "
                + String.join(", ", getProviders().stream().map(BackupProvider::getName).toList())
        ));
    }

}
