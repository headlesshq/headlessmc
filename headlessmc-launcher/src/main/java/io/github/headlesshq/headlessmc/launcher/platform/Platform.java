package io.github.headlesshq.headlessmc.launcher.platform;

import io.github.headlesshq.headlessmc.api.traits.HasName;

public interface Platform extends HasName {
    boolean hasClientSupport();

    boolean hasServerSupport();

    default String getHmcName() {
        return getName();
    }

    default boolean isServerOnly() {
        return hasServerSupport() && !hasClientSupport();
    }

}
