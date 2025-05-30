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
public class AssetSettings extends SettingGroupInitializer {
    private final SettingGroup group = getRoot().group("hmc.assets", "Configure how the launcher downloads assets.");

    private final SettingKey<Long> delay = group.setting(Long.class)
            .withName("hmc.assets.delay")
            .withDescription("Time to wait between the download of individual assets.")
            .withValue(0L)
            .build();

    private final SettingKey<Integer> retries = group.setting(Integer.class)
            .withName("hmc.assets.retries")
            .withDescription("Amount of times to try to redownload an asset that failed to download.")
            .withValue(3)
            .build();

    private final SettingKey<Boolean> parallel = group.setting(Boolean.class)
            .withName("hmc.assets.parallel")
            .withDescription("Downloads Assets on multiple threads at the same time.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> dummyAssets = group.setting(Boolean.class)
            .withName("hmc.assets.dummy")
            .withDescription("Uses small dummy textures and sounds (For optimizing headless mode).")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> checkHash = group.setting(Boolean.class)
            .withName("hmc.assets.checkHash")
            .withAlias("hmc.assets.check.hash")
            .withDescription("Checks the SHA1 hash integrity of an asset when it is downloaded.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> checkSize = group.setting(Boolean.class)
            .withName("hmc.assets.checkSize")
            .withAlias("hmc.assets.check.size")
            .withDescription("Checks that an asset has the correct size when it is downloaded.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> checkFileHash = group.setting(Boolean.class)
            .withName("hmc.assets.checkFileHash")
            .withAlias("hmc.assets.check.file.hash")
            .withDescription("Rechecks the integrity of all assets on the disk before launching the game.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> backoff = group.setting(Boolean.class)
            .withName("hmc.assets.backoff")
            .withDescription("Increases wait time after failed downloads.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> alwaysDownloadAssetIndex = group.setting(Boolean.class)
            .withName("hmc.assets.alwaysDownloadAssetIndex")
            .withAlias("hmc.always.download.assets.index")
            .withDescription("Debugging setting, only for HeadlessMc-CheerpJ, where files corrupt often.")
            .withValue(false)
            .build();

    @Inject
    public AssetSettings(SettingGroup root) {
        super(root);
    }

}
