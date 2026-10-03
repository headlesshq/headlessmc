package io.github.headlesshq.headlessmc.platform.util;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.platform.client.AbstractClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherService;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Implementations of {@link ClientInstaller} and {@link ServerInstaller}
 * need a lot of services to function, resulting in very large
 * constructors.
 * This class collects the most commonly used services
 * into one single {@link Inject}able Bean
 * and implements {@link AbstractClientInstaller.InstallerServices}.
 */
@Getter
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CommonInstallerServices implements AbstractClientInstaller.InstallerServices {
    private final VanillaVersionService vanillaVersionService;
    private final VersionMatcherService versionMatcherService;
    private final JavaLauncherService launcherService;
    private final VersionJsonService versionService;
    private final VanillaInstaller vanillaInstaller;
    private final DownloadService downloadService;
    private final FileService fileService;
    private final AppFiles appFiles;

}
