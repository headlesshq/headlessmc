package io.github.headlesshq.headlessmc.os.config;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSConfigurator;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.util.SequencedSet;

@Dependent
public class ConfigConfigurator implements OSConfigurator {
    private final Holder<CPUConfig> cpuConfig;
    private final Holder<OSConfig> osConfig;

    @Inject
    public ConfigConfigurator(Holder<CPUConfig> cpuConfig, Holder<OSConfig> osConfig) {
        this.cpuConfig = cpuConfig;
        this.osConfig = osConfig;
    }

    @Override
    public OS configure(OS os, SequencedSet<OS> candidates) throws HeadlessMcException {
        OSConfig osConfig = this.osConfig.get();
        String name = osConfig.name().orElse(os.name());
        String version = osConfig.version().orElse(os.version());

        OS.McType mcType = osConfig.type().map(OS.McType::new).orElse(os.type().mcType());
        OS.Type actualType = osConfig.actualType().map(actual -> new OS.Type(actual, mcType))
            .orElse(new OS.Type(os.type().name(), os.type().capitalizedName(), mcType));

        return new OS(name, actualType, version);
    }

    @Override
    public CPU configure(CPU cpu, SequencedSet<CPU> candidates) throws HeadlessMcException {
        CPUConfig cpuConfig = this.cpuConfig.get();
        String arch = cpuConfig.arch().orElse(cpu.architecture());
        CPU.Bitness bitness = cpuConfig.bitness().map(CPU.Bitness::new).orElse(cpu.bitness());

        return new CPU(arch, bitness);
    }

}
