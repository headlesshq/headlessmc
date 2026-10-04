package io.github.headlesshq.headlessmc.distribution;

import java.util.List;
import java.util.Optional;

public interface JavaDistributionProviderService {
    List<JavaDistributionProvider> getProviders();

    Optional<JavaDistributionProvider> getProviderByName(String name);

    JavaDistributionProvider getDefaultProvider();

}
