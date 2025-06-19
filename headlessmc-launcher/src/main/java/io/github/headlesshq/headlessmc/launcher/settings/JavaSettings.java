package io.github.headlesshq.headlessmc.launcher.settings;

import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import io.github.headlesshq.headlessmc.api.settings.SettingGroupInitializer;
import io.github.headlesshq.headlessmc.api.settings.SettingKey;
import io.github.headlesshq.headlessmc.java.JavaDistributionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class JavaSettings extends SettingGroupInitializer {
    private final SettingGroup group = getRoot().group("hmc.java", "Configure how HeadlessMc handles Java.");

    private final SettingKey<List<String>> versions = group.list(String.class)
            .withDelimiter(';')
            .withName("hmc.java.versions")
            .withDescription("A semicolon delimited list of Java versions.")
            .build();

    private final SettingKey<Boolean> useCurrentJava = group.setting(Boolean.class)
            .withName("hmc.java.useCurrentJava")
            .withAlias("hmc.java.use.current")
            .withDescription("Enables HeadlessMc to use the Java version it itself has been launched with to launch games.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> autoDownload = group.setting(Boolean.class)
            .withName("hmc.java.autoDownload")
            .withAlias("hmc.auto.download.java")
            .withDescription("Automatically downloads the required Java version to launch the game.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> autoDownloadThrowExceptions = group.setting(Boolean.class)
            .withName("hmc.java.autoDownloadThrowExceptions")
            .withAlias("hmc.auto.download.java.rethrow.exception")
            .withDescription("Crashes when the download of a Java version fails.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> requireExactJava = group.setting(Boolean.class)
            .withName("hmc.java.requireExactJava")
            .withAlias("hmc.java.require.exact")
            .withDescription("Requires the exact Java version for launching.")
            .withValue(true)
            .build();

    private final SettingKey<String> distribution = group.setting(String.class)
            .withName("hmc.java.downloadDistribution")
            .withAlias("hmc.auto.java.distribution")
            .withDescription("The Java distribution to chose when automatically downloading Java.")
            .withValue(JavaDistributionService.DEFAULT_DISTRIBUTION)
            .build();

    private final SettingKey<Boolean> alwaysAddFilePermissions = group.setting(Boolean.class)
            .withName("hmc.java.alwaysAddFilePermissions")
            .withAlias("hmc.java.always.add.file.permissions")
            .withDescription("Always add certain file permissions when scanning Java versions (Not recommended).")
            .withValue(false)
            .build();

    @Inject
    public JavaSettings(SettingGroup root) {
        super(root);
    }

}
