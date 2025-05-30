package io.github.headlesshq.headlessmc.os;

import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import io.github.headlesshq.headlessmc.api.settings.SettingGroupInitializer;
import io.github.headlesshq.headlessmc.api.settings.SettingKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

@Getter
@ApplicationScoped
public class OSSettings extends SettingGroupInitializer {
    private final SettingGroup group = getRoot().group("hmc.os", "Operating System configuration.");

    private final SettingKey<String> name = group.setting(String.class)
            .withName("hmc.os.name")
            .withAlias("hmc.osname")
            .withDescription("The name of your Operating System.")
            .withValue(c -> requireNonNull(System.getProperty("os.name"), "os.name was null"))
            .build();

    private final SettingKey<OS.Type> type = group.setting(OS.Type.class)
            .withName("hmc.os.type")
            .withAlias("hmc.ostype")
            .withDescription("The name of your Operating System.")
            .withValue(c -> getTypeFromName(c.get(getName())))
            .build();

    private final SettingKey<String> version = group.setting(String.class)
            .withName("hmc.os.version")
            .withAlias("hmc.osversion")
            .withDescription("The version of your Operating System.")
            .withValue(c -> requireNonNull(System.getProperty("os.version"), "os.version was null"))
            .build();

    private final SettingKey<String> architecture = group.setting(String.class)
            .withName("hmc.os.architecture")
            .withAlias("hmc.osarchitecture")
            .withDescription("The name of your CPU architecture.")
            .withValue(c -> getArchitectureFromType(c.get(getType())))
            .build();

    private final SettingKey<Boolean> is64Bit = group.setting(Boolean.class)
            .withName("hmc.os.64bit")
            .withAlias("hmc.osarch")
            .withDescription("Whether your Operating System is 64 bit or not.")
            .withValue(c -> c.get(getArchitecture()).contains("64"))
            .build();

    @Inject
    public OSSettings(SettingGroup root) {
        super(root);
    }

    public OS getOS(Config config) {
        return new OS(
            config.get(getName()),
            config.get(getType()),
            config.get(getVersion()),
            config.get(getArchitecture()),
            config.get(getIs64Bit())
        );
    }

    private OS.Type getTypeFromName(String osName) {
        OS.Type result;
        String os = osName.toLowerCase(Locale.ENGLISH);
        if (os.contains("nux") || os.contains("solaris") || os.contains("nix") || os.contains("sunos")) {
            result = OS.Type.LINUX;
        } else if (os.contains("darwin") || os.contains("mac")) {
            result = OS.Type.OSX;
        } else if (os.contains("win")) {
            result = OS.Type.WINDOWS;
        } else {
            throw new IllegalStateException(
                    "Couldn't detect your Operating System Type from '" + os
                            + "' please provide one of WINDOWS, OSX or LINUX with the "
                            + getType().getName() + " setting!");
        }

        return result;
    }

    private String getArchitectureFromType(OS.Type type) {
        String prop = System.getProperty("os.arch");
        if (prop == null && type == OS.Type.WINDOWS) {
            String p_arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            prop = p_arch == null ? wow64Arch : p_arch;
        }

        return requireNonNull(prop, "Failed to get OS architecture!");
    }

}
