package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configures how server start scripts, like the {@code run.sh}/{@code run.bat}
 * of newer Forge and NeoForge servers, are launched.
 */
@ConfigMapping(prefix = "hmc.server.script")
public interface ScriptConfig extends DynamicConfig {
    /**
     * @return {@code true} if the script should be run with a mock {@code java} command on the {@code PATH},
     * which captures the arguments, so that we can launch the server ourselves.
     * {@code false}, the default, if the script should be run directly.
     */
    @WithDefault("false")
    boolean mockJava();

    /**
     * @return the time in seconds to wait for the script to call the mock {@code java} command.
     * Only used if {@link #mockJava()} is enabled.
     */
    @WithDefault("60")
    int timeout();

}
