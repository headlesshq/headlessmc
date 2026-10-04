package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import io.github.headlesshq.headlessmc.platform.client.AbstractClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.util.CommonInstallerServices;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
@Forge
@ApplicationScoped
@RequiredArgsConstructor
@Getter(AccessLevel.PROTECTED)
public class ForgeInstaller extends AbstractClientInstaller implements ClientInstaller, ServerInstaller {
    private final ForgeInstallerDownloader installerDownloader;
    private final CommonInstallerServices services;
    private final ForgeCLIInstaller cliInstaller;
    private final Cache<PrismIndex> cache;
    private final String platformName;

    @Inject
    public ForgeInstaller(
        @Forge ForgeInstallerDownloader installerDownloader,
        CommonInstallerServices services,
        ForgeCLIInstaller cliInstaller,
        @Forge Cache<PrismIndex> cache
    ) {
        this(installerDownloader, services, cliInstaller, cache, Forge.PLATFORM_NAME);
    }

    @Override
    protected void installClient(VersionID id, Path mcDir, Version vanilla, TypedMap args)
        throws HeadlessMcException {
        log.info("Installing client {} in {} args: {}", id, mcDir, args);
        Path installerJar = installerDownloader.downloadInstaller(id);
        try {
            cliInstaller.installClient(installerJar, mcDir, vanilla.requireJavaVersion());
        } finally {
            services.getFileService().delete(installerJar);
        }
    }

    @Override
    public Installation installServer(
        VersionID id,
        Path dir,
        TypedMap args
    ) throws HeadlessMcException {
        log.info("Installing server {} in {} args: {}", id, dir, args);
        Version vanillaVersion = services.getVanillaInstaller().getVersion(id.getVersion());
        int javaVersion = vanillaVersion.requireJavaVersion();
        Path installerJar = installerDownloader.downloadInstaller(id);
        try {
            services.getLauncherService().buildProcess()
                .id(platformName + "-installer")
                .jar(installerJar)
                .directory(dir)
                .version(javaVersion)
                .arg("--installServer", dir.toAbsolutePath().toString())
                .start()
                .waitFor(ProcessHandler.defaultHandler());
        } finally {
            services.getFileService().delete(installerJar);
        }

        return new Installation(javaVersion);
    }

}
