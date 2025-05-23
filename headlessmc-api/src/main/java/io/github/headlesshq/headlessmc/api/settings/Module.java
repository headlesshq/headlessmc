package io.github.headlesshq.headlessmc.api.settings;

import jakarta.inject.Inject;

public class Module {
    public final SettingGroup root;

    @Inject
    public Module(SettingGroup root) {
        this.root = root;
    }

}
