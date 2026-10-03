package io.github.headlesshq.headlessmc.net.hash;

import lombok.experimental.UtilityClass;

@UtilityClass
final class HexUtil {
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

}
