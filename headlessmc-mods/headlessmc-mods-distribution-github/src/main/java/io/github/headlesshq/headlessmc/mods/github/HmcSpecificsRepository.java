package io.github.headlesshq.headlessmc.mods.github;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The {@code hmc-specifics} mod, distributed by <a href=https://github.com/headlesshq/hmc-specifics>headlesshq/hmc-specifics</a>.
 */
@ApplicationScoped
public class HmcSpecificsRepository extends AbstractHmcModRepository {
    /**
     * The name of this mod in HeadlessMc.
     */
    public static final String NAME = "hmc-specifics";

    /**
     * Constructs a new HmcSpecificsRepository.
     */
    public HmcSpecificsRepository() {
        super(NAME, "hmc-specifics", "Version specific implementations of the HeadlessMc runtime.");
    }

}
