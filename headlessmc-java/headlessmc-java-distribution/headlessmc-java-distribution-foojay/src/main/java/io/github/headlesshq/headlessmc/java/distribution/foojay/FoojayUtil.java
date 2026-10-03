package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.OperatingSystem;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;

/**
 * Utilities for turning objects of the HeadlessMc API into objects of the Foojay API.
 * E.g. {@link OS} to {@link OperatingSystem}.
 */
final class FoojayUtil {
    public static Bitness cpu2Bitness(CPU cpu) {
        if (CPU.Bitness.B64.equals(cpu.bitness())) {
            return Bitness.BIT_64;
        } else if (CPU.Bitness.B32.equals(cpu.bitness())) {
            return Bitness.BIT_32;
        } else {
            return Bitness.NONE;
        }
    }

    public static Architecture cpu2Architecture(CPU cpu) {
        for (Architecture architecture : Architecture.values()) {
            if (architecture.getApiString().equalsIgnoreCase(cpu.architecture())) {
                return architecture;
            }
        }

        return Architecture.NONE;
    }

    public static OperatingSystem os2OperatingSystem(OS os) {
        OperatingSystem byType = null;
        OperatingSystem byApiString = null;
        for (OperatingSystem operatingSystem : OperatingSystem.values()) {
            if (os.name().equalsIgnoreCase(operatingSystem.name())
                || os.name().equalsIgnoreCase(operatingSystem.getUiString())) {
                return operatingSystem;
            }

            if (os.type().name().equalsIgnoreCase(operatingSystem.getUiString())
                || os.type().name().equalsIgnoreCase(operatingSystem.name())) {
                byType = operatingSystem;
            }

            if (os.name().equalsIgnoreCase(operatingSystem.getApiString())
                || os.type().name().equalsIgnoreCase(operatingSystem.getApiString())) {
                byApiString = operatingSystem;
            }
        }

        return byType == null ? (byApiString == null ? OperatingSystem.NONE : byApiString) : byType;
    }

}
