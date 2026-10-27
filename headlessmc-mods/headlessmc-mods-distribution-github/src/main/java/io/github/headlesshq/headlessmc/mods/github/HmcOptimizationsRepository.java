package io.github.headlesshq.headlessmc.mods.github;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The {@code hmc-optimizations} mod, distributed by <a href=https://github.com/headlesshq/hmc-optimizations>headlesshq/hmc-optimizations</a>.
 */
@ApplicationScoped
public class HmcOptimizationsRepository extends AbstractHmcModRepository {
    /**
     * The name of this mod in HeadlessMc.
     */
    public static final String NAME = "hmc-optimizations";

    /**
     * Constructs a new HmcOptimizationsRepository.
     */
    public HmcOptimizationsRepository() {
        super(NAME, "hmc-optimizations", "Version specific optimizations for HeadlessMc.", "hmc_optimizations");
    }

}
