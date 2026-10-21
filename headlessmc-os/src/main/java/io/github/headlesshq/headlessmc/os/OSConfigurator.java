package io.github.headlesshq.headlessmc.os;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.util.Set;


public interface OSConfigurator {
    default OS configure(OS os, Set<OS> candidates) throws HeadlessMcException {
        return os;
    }

    default CPU configure(CPU cpu, Set<CPU> candidates) throws HeadlessMcException {
        return cpu;
    }

}
