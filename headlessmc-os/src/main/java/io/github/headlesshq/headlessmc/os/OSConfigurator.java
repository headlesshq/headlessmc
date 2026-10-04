package io.github.headlesshq.headlessmc.os;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.util.SequencedSet;

public interface OSConfigurator {
    default OS configure(OS os, SequencedSet<OS> candidates) throws HeadlessMcException {
        return os;
    }

    default CPU configure(CPU cpu, SequencedSet<CPU> candidates) throws HeadlessMcException {
        return cpu;
    }

}
