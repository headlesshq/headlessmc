package io.github.headlesshq.headlessmc.os;

import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a CPU architecture.
 *
 * @param architecture the name of the architecture, e.g. x64, or aarch64
 * @param bitness the bitness of the architecture {@link Bitness#B32} or {@link Bitness#B64}.
 * @param instructionSet a common instruction set some architectures share,
 *                       ignoring bitness, <br>e.g. x32 and x64 are both {@code x86}.
 * @param synonyms a (immutable) list of synonym names for this architecture.
 */
public record CPU(String architecture, Bitness bitness, String instructionSet, @Unmodifiable List<String> synonyms) {
    public static final CPU X64 = new CPU("x64", Bitness.B64, "x86", List.of("amd64", "x86_64"));
    public static final CPU X32 = new CPU("x32", Bitness.B32, "x86", List.of("x86", "i386", "i586", "i686"));
    public static final CPU AARCH64 = new CPU("aarch64", Bitness.B64, "arm", List.of("arm64"));
    public static final CPU PPC64 = new CPU("ppc64", Bitness.B64);
    public static final CPU PPC = new CPU("ppc", Bitness.B32);
    public static final CPU RISCV64 = new CPU("riscv64", Bitness.B64);
    public static final CPU IA64 = new CPU("ia64", Bitness.B64);

    // Currently only slight support by our CPU Apache Commons library implementation
    public static final CPU AARCH32 = new CPU("aarch32", Bitness.B32, "arm", List.of("arm", "arm32"));
    public static final CPU PPC64LE = new CPU("ppc64le", Bitness.B64);

    // Currently unsupported by our CPU Apache Commons library implementation,
    // but supported by the foojay java distribution
    public static final CPU ARMHF = new CPU("armhf", Bitness.B32);
    public static final CPU ARMEL = new CPU("armel", Bitness.B32);
    public static final CPU MIPS = new CPU("mips", Bitness.B32);
    public static final CPU MIPSEL = new CPU("mipsel", Bitness.B32);
    public static final CPU S390X = new CPU("s390x", Bitness.B64);
    public static final CPU SPARCV9 = new CPU("sparcv9", Bitness.B64);
    public static final CPU SPARC = new CPU("sparc", Bitness.B32);

    // Unsupported by foojay java distribution service, but supported by Apache Commons
    public static final CPU RISCV32 = new CPU("riscv32", Bitness.B32);

    public static final CPU UNKNOWN = new CPU("unknown", Bitness.UNKNOWN);

    public CPU(String architecture, Bitness bitness, String... synonyms) {
        this(architecture, bitness, architecture, Arrays.asList(synonyms));
    }

    public record Bitness(int bits) {
        public static final Bitness B64 = new Bitness(64);
        public static final Bitness B32 = new Bitness(32);
        public static final Bitness UNKNOWN = new Bitness(0);
    }

}
