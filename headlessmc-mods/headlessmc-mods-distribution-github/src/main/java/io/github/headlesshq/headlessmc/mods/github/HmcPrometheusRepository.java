package io.github.headlesshq.headlessmc.mods.github;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The {@code hmc-prometheus} mod, distributed by <a href=https://github.com/headlesshq/hmc-prometheus>headlesshq/hmc-prometheus</a>.
 */
@ApplicationScoped
public class HmcPrometheusRepository extends AbstractHmcSpecificsAddonRepository {
    /**
     * The name of this mod in HeadlessMc.
     */
    public static final String NAME = "hmc-prometheus";

    /**
     * Constructs a new HmcPrometheusRepository.
     */
    public HmcPrometheusRepository() {
        super(NAME, "hmc-prometheus", "hmc-prometheus", "A prometheus exporter for the Minecraft client.", "hmc_prometheus");
    }

}
