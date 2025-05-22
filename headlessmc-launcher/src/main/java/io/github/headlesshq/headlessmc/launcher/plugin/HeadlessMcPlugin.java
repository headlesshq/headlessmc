package io.github.headlesshq.headlessmc.launcher.plugin;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import org.jetbrains.annotations.NotNull;

public interface HeadlessMcPlugin extends HasName, HasDescription, Comparable<HeadlessMcPlugin> {
    int getPriority();

    void init(Launcher launcher);

    @Override
    default int compareTo(@NotNull HeadlessMcPlugin o) {
        int result = Integer.compare(this.getPriority(), o.getPriority());
        return result == 0 ? this.getName().compareTo(o.getName()) : result;
    }

}
