package io.github.headlesshq.headlessmc.java.service;

import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.java.JavaVersionParser;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.inject.Inject;

public class FilePermissionJavaParser extends JavaVersionParser {
    @Inject
    public FilePermissionJavaParser(Config cfg, JavaSettings settings, OS os) {
        super(shouldAddFilePermissions(cfg, settings, os));
    }

    private static boolean shouldAddFilePermissions(Config cfg, JavaSettings settings, OS os) {
        boolean addFilePermissions = os.getType() == OS.Type.LINUX || os.getType() == OS.Type.OSX;
        addFilePermissions &= cfg.get(settings.alwaysAddFilePermissions());
        return addFilePermissions;
    }

}
