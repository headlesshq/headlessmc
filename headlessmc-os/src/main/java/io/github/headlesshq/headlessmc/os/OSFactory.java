package io.github.headlesshq.headlessmc.os;

import lombok.experimental.UtilityClass;
import lombok.val;
import io.github.headlesshq.headlessmc.api.config.Config;
import io.github.headlesshq.headlessmc.api.config.Property;

import java.util.Locale;
import java.util.Objects;

@UtilityClass
// TODO: this, especially arch detection, can probably be improved
public class OSFactory {
    public static OS detect(Config config) {
        val name = getName(config);
        val version = getVersion(config);
        val type = getType(config, name);
        val architecture = getArchitecture(config, type);
        val arch = is64Bit(config, type);

        return new OS(name, type, version, architecture, arch);
    }

    private static String getName(Config config) {
        return getGeneric(config, OsProperties.OS_NAME, "os.name");
    }

    private static String getVersion(Config config) {
        return getGeneric(config, OsProperties.OS_VERSION, "os.version");
    }

    private static OS.Type getType(Config config, String osIn) {
        val type = config.get(OsProperties.OS_TYPE);
        if (type != null) {
            return OS.Type.valueOf(type.toUpperCase(Locale.ENGLISH));
        }

        OS.Type result;
        String os = osIn;
        if ((os = os.toLowerCase(Locale.ENGLISH)).contains("nux")
            || os.contains("solaris")
            || os.contains("nix")
            || os.contains("sunos")) {
            result = OS.Type.LINUX;
        } else if (os.contains("darwin") || os.contains("mac")) {
            result = OS.Type.OSX;
        } else if (os.contains("win")) {
            result = OS.Type.WINDOWS;
        } else {
            throw new IllegalStateException(
                "Couldn't detect your Operating System Type from '" + os
                    + "' please provide one of WINDOWS, OSX or LINUX with the "
                    + OsProperties.OS_TYPE + " property!");
        }

        return result;
    }

    private static String getArchitecture(Config config, OS.Type type) {
        String architecture = config.get(OsProperties.OS_ARCHITECTURE);
        if (architecture == null) {
            if (type == OS.Type.WINDOWS) {
                String processorArch = System.getenv("PROCESSOR_ARCHITECTURE");
                String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
                return processorArch == null
                        ? (wow64Arch == null
                            ? Objects.requireNonNull(System.getProperty("os.arch"), "Couldn't detect OS architecture")
                            : wow64Arch)
                        : processorArch;
            } else {
                architecture = Objects.requireNonNull(System.getProperty("os.arch"), "Couldn't detect OS architecture");
            }
        }

        return architecture;
    }

    private static boolean is64Bit(Config config, OS.Type type) {
        Boolean arch = config.get(OsProperties.OS_ARCH);
        if (arch == null) {
            if (type == OS.Type.WINDOWS) {
                String processorArch = System.getenv("PROCESSOR_ARCHITECTURE");
                String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

                arch = processorArch != null && processorArch.endsWith("64")
                    || wow64Arch != null && wow64Arch.endsWith("64");
            } else {
                arch = Objects.requireNonNull(
                                  System.getProperty("os.arch"),
                                  "Couldn't detect if your OS uses 64 bit!")
                              .contains("64");
            }
        }

        return arch;
    }

    private static String getGeneric(Config config,
                                     Property<String> property,
                                     String systemProp) {
        String result = config.get(property);
        if (result == null) {
            result = System.getProperty(systemProp);
        }

        return Objects.requireNonNull(result, String.format(
            "Couldn't find %s, please provide it using the config!",
            property.getName()));
    }

}
