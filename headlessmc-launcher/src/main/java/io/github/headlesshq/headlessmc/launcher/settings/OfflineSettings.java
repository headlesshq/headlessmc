package io.github.headlesshq.headlessmc.launcher.settings;

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
public class OfflineSettings extends SettingGroupInitializer {
    private final SettingGroup group = getRoot().group("hmc.offline", "Configure the account to use when offline");

    private final SettingKey<Boolean> offline = group.setting(Boolean.class)
            .withName("hmc.offline")
            .withDescription("Enables Offline mode.")
            .withValue(false)
            .build();

    private final SettingKey<String> offlineType = group.setting(String.class)
            .withName("hmc.offline")
            .withDescription("Type of the offline account.")
            .withValue("msa")
            .build();

    private final SettingKey<String> offlineUUID = group.setting(String.class)
            .withName("hmc.offline.uuid")
            .withDescription("UUID to use when offline.")
            .withValue("22689332a7fd41919600b0fe1135ee34")
            .build();

    private final SettingKey<String> offlineUserName = group.setting(String.class)
            .withName("hmc.offline.username")
            .withDescription("Name to use when offline.")
            .withValue("Offline")
            .build();

    private final SettingKey<String> offlineToken = group.setting(String.class)
            .withName("hmc.offline.token")
            .withDescription("Token to use when offline.")
            .withValue("")
            .build();

    @Inject
    public OfflineSettings(SettingGroup root) {
        super(root);
    }

}
