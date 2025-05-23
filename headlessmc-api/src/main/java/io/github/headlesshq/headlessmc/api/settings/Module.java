package io.github.headlesshq.headlessmc.api.settings;

import jakarta.inject.Inject;
import lombok.Getter;

@Getter
public class Module {
    private final SettingGroup root;

    @Inject
    public Module(SettingGroup root) {
        this.root = root;
    }

}
