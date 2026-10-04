package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "hmc.launcher")
public interface LauncherConfig extends DynamicConfig {
    @WithDefault("false")
    boolean crashReportWatcher();

    @WithDefault("true")
    boolean autoInstall();

    @WithDefault("true")
    boolean free();

}
