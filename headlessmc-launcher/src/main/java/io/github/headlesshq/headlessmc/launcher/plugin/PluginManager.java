package io.github.headlesshq.headlessmc.launcher.plugin;

import io.github.headlesshq.headlessmc.launcher.Launcher;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Getter
public class PluginManager {
    private final List<HeadlessMcPlugin> plugins = new ArrayList<>();

    public void init(Launcher launcher) {
        ServiceLoader.load(HeadlessMcPlugin.class).forEach(plugins::add);
        plugins.forEach(plugin -> plugin.init(launcher));
    }

}
