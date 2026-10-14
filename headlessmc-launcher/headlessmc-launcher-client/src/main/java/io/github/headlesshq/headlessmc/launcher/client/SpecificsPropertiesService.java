package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.config.ConfigService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Forwards the {@link SpecificsProperty SpecificsProperties} from the HeadlessMc config
 * to the game, as SystemProperties for hmc-specifics.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class SpecificsPropertiesService {
    private final ConfigService configService;

    /**
     * @return SystemProperties for every {@link SpecificsProperty}
     * whose configured value deviates from the default value of hmc-specifics.
     */
    public Map<String, String> getSystemProperties() {
        return getSystemProperties(configService.getConfig());
    }

    static Map<String, String> getSystemProperties(Config config) {
        Map<String, String> result = new LinkedHashMap<>();
        for (SpecificsProperty property : SpecificsProperty.ALL) {
            Optional<?> value;
            try {
                value = config.getOptionalValue(property.name(), property.type());
            } catch (IllegalArgumentException | NoSuchElementException e) {
                log.warn("Invalid value for {}, not passing it to the game: {}", property.name(), e.getMessage());
                continue;
            }

            // values are converted by the config, so e.g. "on" ends up as "true", which hmc-specifics understands
            value.filter(v -> !Objects.equals(v, property.defaultValue()))
                .ifPresent(v -> result.put(property.name(), v.toString()));
        }

        return result;
    }

}
