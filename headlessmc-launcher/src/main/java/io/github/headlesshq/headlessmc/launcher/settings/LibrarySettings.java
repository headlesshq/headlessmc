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
public class LibrarySettings extends SettingGroupInitializer {
    private final SettingGroup group = getRoot().group("hmc.libraries", "Configure how HeadlessMc downloads libraries.");

    private final SettingKey<Boolean> checkHash = group.setting(Boolean.class)
            .withName("hmc.libraries.checkHash")
            .withAlias("hmc.libraries.check.hash")
            .withDescription("Checks the SHA1 hash integrity of a library when it is downloaded.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> checkSize = group.setting(Boolean.class)
            .withName("hmc.libraries.checkSize")
            .withAlias("hmc.libraries.check.size")
            .withDescription("Checks that a library has the correct size when it is downloaded.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> checkFileHash = group.setting(Boolean.class)
            .withName("hmc.libraries.checkFileHash")
            .withAlias("hmc.libraries.check.file.hash")
            .withDescription("Rechecks the integrity of all libraries on the disk before launching the game.")
            .withValue(false)
            .build();

    @Inject
    public LibrarySettings(SettingGroup root) {
        super(root);
    }

}
