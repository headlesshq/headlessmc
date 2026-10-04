package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Fabric
@ApplicationScoped
@RequiredArgsConstructor
@Getter(AccessLevel.PROTECTED)
class FabricInstaller extends AbstractClientInstaller implements ClientInstaller, ServerInstaller {
    private final FabricInstallerDownloader installerDownloader;
    private final CommonInstallerServices services;
    private final String platformName;

    @Inject
    public FabricInstaller(FabricInstallerDownloader installerDownloader, CommonInstallerServices services) {
        this(installerDownloader, services, Fabric.PLATFORM_NAME);
    }

    @Override
    public Installation installServer(VersionID id, Path dir, TypedMap args) throws HeadlessMcException {
        Installation vanillaInstallation = services.getVanillaInstaller()
            .installServer(id.asVanillaVersion(), dir, args);

        install(id, dir, args, true, vanillaInstallation.javaVersion());
        return new Installation(vanillaInstallation.javaVersion());
    }

    @Override
    protected void installClient(VersionID id, Path mcDir, Version vanilla, TypedMap args) throws HeadlessMcException {
        install(id, mcDir, args, false, vanilla.requireJavaVersion());
    }

    private void install(VersionID id, Path dir, TypedMap args, boolean server, int javaVersion)
        throws HeadlessMcException {
        Path installer = installerDownloader.download(id, args.get(CUSTOM_INSTALLER_URL));
        services.getLauncherService().buildProcess()
            .id(platformName + "-installer")
            .jar(installer)
            .arg(getArgs(id, dir, server).toArray(String[]::new))
            .version(javaVersion)
            //.jvmArg()
            .start()
            .waitFor(ProcessHandler.defaultHandler());
    }

    private List<String> getArgs(VersionID id, Path dir, boolean server) {
        List<String> args = new ArrayList<>();
        if (server) {
            args.add("server");
        } else {
            args.add("client");
            // TODO: make configurable
            args.add("-noprofile");
        }

        args.add("-mcversion");
        args.add(id.getVersion().getName());
        id.getBuild().ifPresent(loader -> {
            args.add("-loader");
            args.add(loader.getName());
        });

        args.add("-dir");
        args.add(dir.toAbsolutePath().toString());
        return args;
    }

}
