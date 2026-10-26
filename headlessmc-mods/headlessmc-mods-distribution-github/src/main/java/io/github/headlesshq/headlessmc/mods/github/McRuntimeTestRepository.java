package io.github.headlesshq.headlessmc.mods.github;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The {@code mc-runtime-test} mod, distributed by <a href=https://github.com/headlesshq/mc-runtime-test-mod>headlesshq/mc-runtime-test-mod</a>.
 */
@ApplicationScoped
public class McRuntimeTestRepository extends AbstractHmcModRepository {
    /**
     * The name of this mod in HeadlessMc.
     */
    public static final String NAME = "mc-runtime-test";

    /**
     * Constructs a new McRuntimeTestRepository.
     */
    public McRuntimeTestRepository() {
        super(NAME, "mc-runtime-test-mod", "Test the Minecraft client inside your CI.");
    }

}
