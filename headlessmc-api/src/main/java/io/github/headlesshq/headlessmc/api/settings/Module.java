package io.github.headlesshq.headlessmc.api.settings;

public class Module {
    public final SettingGroup group;

    public Module(Module module) {
        this(module.group);
    }

    public Module(SettingGroup group) {
        this.group = group;
    }

}
