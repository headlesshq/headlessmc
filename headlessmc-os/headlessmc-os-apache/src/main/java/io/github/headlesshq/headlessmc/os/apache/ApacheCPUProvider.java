package io.github.headlesshq.headlessmc.os.apache;

import io.github.headlesshq.headlessmc.os.CPU;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArchUtils;
import org.apache.commons.lang3.SystemProperties;
import org.apache.commons.lang3.arch.Processor;

@Slf4j
@Dependent
public class ApacheCPUProvider {
    @Produces
    @Dependent
    @ApacheCommons
    public CPU.Bitness detectBitness() {
        Processor processor = ArchUtils.getProcessor();
        return Processor.Arch.BIT_64.equals(processor.getArch())
            ? CPU.Bitness.B64
            : Processor.Arch.BIT_32.equals(processor.getArch())
                ? CPU.Bitness.B32
                : CPU.Bitness.UNKNOWN;
    }

    @Produces
    @Dependent
    @ApacheCommons
    public CPU detectCPU(@ApacheCommons CPU.Bitness bitness) {
        if (CPU.Bitness.UNKNOWN.equals(bitness)) {
            return CPU.UNKNOWN;
        }

        Processor processor = ArchUtils.getProcessor();
        String property = SystemProperties.getOsArch();
        // Apache does not seem to handle AARCH32
        if ("aarch32".equalsIgnoreCase(property)
            || "arm32".equalsIgnoreCase(property)
            || ("arm".equalsIgnoreCase(property)
                && !Processor.Type.AARCH_64.equals(processor.getType()))) {
            return CPU.AARCH32;
        }

        if ("ppc64le".equalsIgnoreCase(property)) {
            return CPU.PPC64LE;
        }

        switch (processor.getType()) {
            case AARCH_64 -> {
                // does Apache handle it this way?
                // do they return AARCH_64 with 32 bitness?
                return bitness.bits() == 64
                    ? CPU.AARCH64
                    : CPU.AARCH32;
            }
            case X86 -> {
                return bitness.bits() == 64
                    ? CPU.X64
                    : CPU.X32;
            }
            case IA_64 -> {
                if (bitness.bits() == 64) {
                    return CPU.IA64;
                }

                log.error("IA64 architecture but bitness: {}", bitness);
                return CPU.UNKNOWN;
            }
            case PPC -> {
                return bitness.bits() == 64
                    ? CPU.PPC64
                    : CPU.PPC;
            }
            case RISC_V -> {
                return bitness.bits() == 64
                    ? CPU.RISCV64
                    : CPU.RISCV32;
            }
            case UNKNOWN -> {
                return new CPU("unknown", bitness);
            }
        }

        return new CPU("unknown", bitness);
    }

}
