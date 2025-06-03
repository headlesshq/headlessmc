package io.github.headlesshq.headlessmc.commons;

import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import io.github.headlesshq.headlessmc.api.settings.SettingGroupInitializer;
import io.github.headlesshq.headlessmc.api.settings.SettingKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@ApplicationScoped
@Accessors(fluent = true)
public class DownloadSettings extends SettingGroupInitializer {
    private final SettingGroup group = getRoot().group("hmc.launcher", "Launcher settings");

    private final SettingKey<Boolean> httpUserAgentEnabled = group.setting(Boolean.class)
            .withName("hmc.launcher.httpUserAgentEnabled")
            .withAlias("hmc.http.user.agent.enabled")
            .withDescription("Adds a user agent to http requests when downloading files.")
            .withValue(true)
            .build();

    private final SettingKey<String> httpUserAgent = group.setting(String.class)
            .withName("hmc.launcher.httpUserAgent")
            .withAlias("hmc.http.user.agent")
            .withDescription("Name of the user agent to add to http requests.")
            .withValue("Mozilla/5.0")
            .build();

    @Inject
    public DownloadSettings(SettingGroup root) {
        super(root);
    }

}
