package io.github.headlesshq.headlessmc.os;

/**
 * The C standard library of a Linux system or process.
 * Native executables and libraries, e.g. Java runtimes or the natives of LWJGL,
 * only run on the C standard library they have been built against.
 *
 * @see LibCDetector
 */
public enum LibC {
    /**
     * The GNU C Library, used by most Linux distributions, e.g. Debian, Ubuntu, Fedora and Arch Linux.
     */
    GLIBC,
    /**
     * The musl C library, used e.g. by Alpine Linux, Void Linux (musl) and Chimera Linux.
     */
    MUSL,
    /**
     * The C standard library could not be detected, e.g. because this is not a Linux system.
     */
    UNKNOWN
}
