package io.github.headlesshq.headlessmc.version;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

@FunctionalInterface
public interface VersionProcessor {
    ProcessedVersion process(Version version) throws HeadlessMcException;

}
