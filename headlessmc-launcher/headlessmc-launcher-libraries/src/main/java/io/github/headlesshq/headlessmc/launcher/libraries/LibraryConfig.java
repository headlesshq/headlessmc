package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.net.parallel.ParallelConfig;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "hmc.libraries")
public interface LibraryConfig extends DynamicConfig {
    /** Parallel download config for libraries, nested under {@code hmc.libraries.parallel}. */
    ParallelConfig parallel();

}
