package io.github.headlesshq.headlessmc.os;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents an Operating System.
 */
@Data
@RequiredArgsConstructor
public class OS implements HasName {
    private final String name;
    private final Type type;
    private final String version;
    private final String architecture;
    private final boolean b64;

    public boolean isArm() {
        return "arm64".equalsIgnoreCase(architecture) || "aarch64".equalsIgnoreCase(architecture);
    }

    public boolean is64bit() {
        return b64;
    }

    @Getter
    public enum Type implements HasName {
        LINUX("linux", "nux", "solaris", "nix", "sunos"),
        OSX("osx", "darwin", "mac"),
        WINDOWS("windows", "win"),
        UNKNOWN("unknown");

        private final String name;
        private final String[] patterns;

        Type(String name, String... patterns) {
            this.name = name;
            this.patterns = patterns;
        }

        public boolean matches(String osName) {
            for (String pattern : patterns) {
                if (osName.contains(pattern)) {
                    return true;
                }
            }
            return false;
        }
    }

}
