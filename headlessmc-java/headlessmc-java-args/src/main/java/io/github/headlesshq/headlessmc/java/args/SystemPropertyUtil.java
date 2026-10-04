package io.github.headlesshq.headlessmc.java.args;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class SystemPropertyUtil {
    public static boolean isSystemProperty(String string) {
        return string.startsWith("-D");
    }

    public static ArgPair parseSystemProperty(String string) {
        String keyValue = string;
        if (keyValue.startsWith("-D")) {
            keyValue = keyValue.substring(2);
        }

        String[] split = keyValue.split("=", 2);
        if (split.length == 1) {
            return new ArgPair(split[0], null);
        }

        return new ArgPair(split[0], split[1]);
    }

}
