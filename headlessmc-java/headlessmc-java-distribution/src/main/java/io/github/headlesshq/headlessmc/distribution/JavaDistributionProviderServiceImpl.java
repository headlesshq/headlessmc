package io.github.headlesshq.headlessmc.distribution;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JavaDistributionProviderServiceImpl implements JavaDistributionProviderService {
    private final Instance<JavaDistributionProvider> services;
    private final JavaDistributionProvider defaultService;

    @Inject
    public JavaDistributionProviderServiceImpl(
        @Any Instance<JavaDistributionProvider> services,
        JavaDistributionProvider defaultService
    ) {
        this.services = services;
        this.defaultService = defaultService;
    }

    @Override
    public List<JavaDistributionProvider> getProviders() {
        return services.stream().toList();
    }

    @Override
    public Optional<JavaDistributionProvider> getProviderByName(String name) {
        return services.stream()
            .filter(service -> service.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    public JavaDistributionProvider getDefaultProvider() {
        return defaultService;
    }

}
