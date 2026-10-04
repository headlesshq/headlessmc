package io.github.headlesshq.headlessmc.forge.installer;

import java.io.File;

public interface InstallationStrategy {
    void install(File target) throws Exception;

    boolean isUsable();

}
