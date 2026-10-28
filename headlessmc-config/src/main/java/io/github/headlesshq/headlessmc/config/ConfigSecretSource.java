package io.github.headlesshq.headlessmc.config;

import io.github.headlesshq.headlessmc.console.output.SecretSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Supplies the values of config properties that hold secrets, like tokens or api keys,
 * so that they are never printed.
 * Properties are recognized as secret by their name,
 * independent of their case and of {@code -} and {@code _} being used instead of {@code .},
 * so that environment variables like {@code GH_TOKEN} are recognized as well.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConfigSecretSource implements SecretSource {
    private static final List<String> SECRET_SUFFIXES = List.of(".token", ".api.key", ".password", ".secret");

    private final ConfigService configService;

    /**
     * @param property the name of a config property.
     * @return {@code true} if the property holds a secret.
     */
    public boolean isSecret(String property) {
        String normalized = property.toLowerCase(Locale.ENGLISH).replace('_', '.').replace('-', '.');
        return SECRET_SUFFIXES.stream().anyMatch(normalized::endsWith);
    }

    @Override
    public Collection<String> getSecrets() {
        return configService.bind(ConfigSecretSource.class, this::computeSecrets);
    }

    private List<String> computeSecrets() {
        return configService.getPropertyNames()
            .stream()
            .filter(this::isSecret)
            .map(name -> configService.getConfig().getOptionalValue(name, String.class))
            .flatMap(Optional::stream)
            .toList();
    }

}
