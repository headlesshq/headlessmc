package io.github.headlesshq.headlessmc.os;

import java.util.Locale;

/**
 * Represents an Operating System.
 *
 * @param name    the name of the operating system.
 * @param type
 * @param version
 */
public record OS(String name, Type type, String version) {
    /**
     * The type of the Operating System.
     * A library may create its own type if it wants to add support
     * for another operating system.
     * But it then needs to register handlers to handle the new types,
     * e.g. to use the Java downloading APIs.
     * // TODO: list of handlers
     *
     * @param name            the name of the Operating system, lower case.
     * @param capitalizedName the name of the Operating system, with proper capitalization.
     * @param mcType          the {@link McType} of the operating system.
     */
    public record Type(String name, String capitalizedName, McType mcType) {
        public static final Type WINDOWS = new Type("Windows", McType.WINDOWS);
        public static final Type MACOS = new Type("macOS", McType.OSX);
        public static final Type LINUX = new Type("Linux", McType.LINUX);

        // The following Operating Systems are not really Linux, but we handle them as McType.LINUX
        // They are only supported for Java downloads, but unsupported for mc launching.
        // Libraries may add support for the operating systems.
        public static final Type FREE_BSD = new Type("FreeBSD", McType.LINUX);
        public static final Type SOLARIS = new Type("Solaris", McType.LINUX);
        public static final Type AIX = new Type("AIX", McType.LINUX);

        // Special support is required for Android which is not yet implemented,
        // but we can detect it.
        public static final Type ANDROID = new Type("Android", McType.LINUX);

        // The following types of OS have no support in our current OS library, but are supported
        // the foojay java distribution service
        // public static final Type SUN = new Type("SUN", McType.LINUX);
        public static final Type QNX = new Type("QNX", McType.LINUX);
        public static final Type IOS = new Type("iOS", McType.OSX);
        public static final Type ALPINE_LINUX = new Type("Alpine Linux", McType.LINUX);
        public static final Type LINUX_MUSL = new Type("Linux Musl", McType.LINUX);
        public static final Type BROWSER = new Type("Browser", McType.LINUX);
        public static final Type UNKNOWN = new Type("Unknown", McType.UNKNOWN);

        public Type(String capitalizedName, McType mcType) {
            this(capitalizedName.toLowerCase(Locale.ENGLISH), capitalizedName, mcType);
        }
    }

    /**
     * The name of the Operating system as it occurs in mc version.json files.
     * These are usually used to identify native libraries and for rules
     * to allow/disallow certain arguments or libraries.
     * In the official mc version.jsons only
     * {@code "windows", "linux", and "osx"} appear.
     * A library may create its own {@code McType},
     * but it then needs to also register handlers
     * to ensure they are handled properly in e.g. library resolution.
     * // TODO: list of handlers typically required
     *
     * @param name the name of the operating system.
     */
    public record McType(String name) {
        public static final McType WINDOWS = new McType("windows");
        public static final McType LINUX = new McType("linux");
        public static final McType OSX = new McType("osx");
        public static final McType UNKNOWN = new McType("Unknown");
    }

}
