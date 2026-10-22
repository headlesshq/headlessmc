package io.github.headlesshq.headlessmc.auth.cdi;

import io.github.headlesshq.headlessmc.auth.yggdrasil.YggdrasilConfig;
import io.github.headlesshq.headlessmc.config.ConfigService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Default {@link YggdrasilConfig} implementation, reading
 * {@value #URL_PROPERTY} from the HeadlessMc config.
 */
@RequiredArgsConstructor
public class YggdrasilConfigImpl implements YggdrasilConfig {
    /**
     * The property holding the default Yggdrasil server url.
     * Yggdrasil authentication is disabled unless this is set.
     */
    public static final String URL_PROPERTY = "hmc.auth.yggdrasil.url";

    private final ConfigService configService;

    @Override
    public Optional<String> url() {
        return configService.getConfig().getOptionalValue(URL_PROPERTY, String.class)
            .map(String::trim)
            .filter(url -> !url.isEmpty());
    }

}
