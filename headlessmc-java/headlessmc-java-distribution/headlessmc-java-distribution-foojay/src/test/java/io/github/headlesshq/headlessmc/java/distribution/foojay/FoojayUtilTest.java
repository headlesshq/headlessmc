package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.OperatingSystem;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FoojayUtilTest {
    @Test
    public void testCPU2Bitness() {
        assertEquals(Bitness.BIT_32, FoojayUtil.cpu2Bitness(CPU.AARCH32));
        assertEquals(Bitness.BIT_64, FoojayUtil.cpu2Bitness(CPU.AARCH64));
        assertEquals(Bitness.NONE, FoojayUtil.cpu2Bitness(CPU.UNKNOWN));
    }

    @Test
    public void testCPUToArchitecture() {
        assertEquals(Architecture.AARCH64, FoojayUtil.cpu2Architecture(CPU.AARCH64));
        assertEquals(Architecture.AARCH32, FoojayUtil.cpu2Architecture(CPU.AARCH32));
        assertEquals(Architecture.X64, FoojayUtil.cpu2Architecture(CPU.X64));
        assertEquals(Architecture.X32, FoojayUtil.cpu2Architecture(CPU.X32));
        assertEquals(Architecture.ARMHF, FoojayUtil.cpu2Architecture(CPU.ARMHF));
        assertEquals(Architecture.ARMEL, FoojayUtil.cpu2Architecture(CPU.ARMEL));
        assertEquals(Architecture.MIPS, FoojayUtil.cpu2Architecture(CPU.MIPS));
        assertEquals(Architecture.MIPSEL, FoojayUtil.cpu2Architecture(CPU.MIPSEL));
        assertEquals(Architecture.PPC64LE, FoojayUtil.cpu2Architecture(CPU.PPC64LE));
        assertEquals(Architecture.PPC64, FoojayUtil.cpu2Architecture(CPU.PPC64));
        assertEquals(Architecture.PPC, FoojayUtil.cpu2Architecture(CPU.PPC));
        assertEquals(Architecture.RISCV64, FoojayUtil.cpu2Architecture(CPU.RISCV64));
        assertEquals(Architecture.S390X, FoojayUtil.cpu2Architecture(CPU.S390X));
        assertEquals(Architecture.S390X, FoojayUtil.cpu2Architecture(CPU.S390X));
        assertEquals(Architecture.SPARCV9, FoojayUtil.cpu2Architecture(CPU.SPARCV9));
        assertEquals(Architecture.SPARC, FoojayUtil.cpu2Architecture(CPU.SPARC));
        assertEquals(Architecture.NONE, FoojayUtil.cpu2Architecture(CPU.UNKNOWN));
    }

    private OS os(OS.Type type) {
        return new OS(type.name(), type, "1.0");
    }

    @Test
    public void testOSToOperatingSystem() {
        assertEquals(OperatingSystem.ALPINE_LINUX, FoojayUtil.os2OperatingSystem(os(OS.Type.ALPINE_LINUX)));
        assertEquals(OperatingSystem.LINUX_MUSL, FoojayUtil.os2OperatingSystem(os(OS.Type.LINUX_MUSL)));
        assertEquals(OperatingSystem.LINUX, FoojayUtil.os2OperatingSystem(os(OS.Type.LINUX)));
        assertEquals(OperatingSystem.FREE_BSD, FoojayUtil.os2OperatingSystem(os(OS.Type.FREE_BSD)));
        assertEquals(OperatingSystem.MACOS, FoojayUtil.os2OperatingSystem(os(OS.Type.MACOS)));
        assertEquals(OperatingSystem.WINDOWS, FoojayUtil.os2OperatingSystem(os(OS.Type.WINDOWS)));
        assertEquals(OperatingSystem.SOLARIS, FoojayUtil.os2OperatingSystem(os(OS.Type.SOLARIS)));
        assertEquals(OperatingSystem.QNX, FoojayUtil.os2OperatingSystem(os(OS.Type.QNX)));
        assertEquals(OperatingSystem.AIX, FoojayUtil.os2OperatingSystem(os(OS.Type.AIX)));
        assertEquals(OperatingSystem.NONE, FoojayUtil.os2OperatingSystem(os(OS.Type.UNKNOWN)));
    }

}
