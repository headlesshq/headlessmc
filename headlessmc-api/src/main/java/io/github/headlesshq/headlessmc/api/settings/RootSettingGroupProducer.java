package io.github.headlesshq.headlessmc.api.settings;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class RootSettingGroupProducer {
    @Produces
    @ApplicationScoped
    public SettingGroup get() {
        return new SettingGroupImpl("hmc", "HeadlessMc settings.");
    }

}
