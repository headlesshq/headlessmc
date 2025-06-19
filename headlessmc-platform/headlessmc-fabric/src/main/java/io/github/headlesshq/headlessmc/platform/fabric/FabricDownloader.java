package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.files.FileManager;
import io.github.headlesshq.headlessmc.files.HeadlessMcDir;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncher;
import io.github.headlesshq.headlessmc.platform.AbstractPlatformDownloader;
import io.github.headlesshq.headlessmc.platform.PlatformDownloadOptions;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import io.github.headlesshq.headlessmc.platform.vanilla.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.vanilla.VanillaVersionManager;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.CustomLog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Fabric
@CustomLog
@ApplicationScoped
public class FabricDownloader extends AbstractPlatformDownloader implements PlatformDownloader {
    private final JavaLauncher javaLauncher;
    private final FileManager files;

    @Inject
    public FabricDownloader(VanillaVersionManager vanillaVersionManager,
                            DownloadService downloadService,
                            VersionService versionService,
                            @HeadlessMcDir FileManager files,
                            JavaLauncher javaLauncher) { // TODO: why not proxiable?
        super(vanillaVersionManager, downloadService, versionService);
        this.files = files;
        this.javaLauncher = javaLauncher;
    }

    @Override
    protected Path download(Version vanillaV, PlatformDownloadOptions optionsIn, Path versionJson) throws IOException {
        FabricDownloadOptions options = FabricDownloadOptions.from(optionsIn);
        FabricInstaller installer = getInstaller(options);
        javaLauncher.createProcess()
                .jar(installer.getFile(files.get("fabric"), downloadService))
                .jvmArgs(options.getJvmArgs())
                .version(getJava(vanillaV, options))
                .args(getArgs(options))
                .inheritIO() // TODO: configurable
                .launch()
                .waitForSuccess();

        versionService.refresh();
        Version version = versionService.get(options.getVersionID());
        if (version == null) {
            throw new IOException("Failed to find installed version " + options.getVersionID());
        }

        return null;
    }

    private int getJava(Version vanillaVersion, FabricDownloadOptions options) throws IOException {
        if (options.getJava() != null) {
            return options.getJava();
        }

        Integer vanillaVJava = vanillaVersion.getJava();
        if (vanillaVJava == null) {
            throw new IOException("Failed to get Java version from vanilla version " + vanillaVersion);
        }

        return vanillaVJava;
    }

    private FabricInstaller getInstaller(FabricDownloadOptions options) {
        if (options.getUrl() != null) {
            return new FabricInstaller("fabric-installer-custom.jar", options.getUrl(), null, null, true);
        }

        VanillaVersion legacy = vanillaVersionManager.getVersion("1.14");
        VanillaVersion vanilla = vanillaVersionManager.getVersion(options.getVersionID().getVersion());
        if (legacy != null && vanilla != null) {
            if (vanillaVersionManager.isOlderThan(legacy, vanilla)) {
                return FabricInstaller.legacy();
            }
        } else {
            log.warn("Failed to compare 1.14 to " + options.getVersionID().getVersion() + ":" + legacy + "," + vanilla);
        }

        return FabricInstaller.defaultInstaller();
    }

    protected List<String> getArgs(FabricDownloadOptions options) {
        List<String> args = new ArrayList<>();
        if (options.getVersionID().isServer()) {
            args.add("server");
        } else {
            args.add("client");
            args.add("-noprofile"); // TODO: make this configurable?
        }

        args.add("-mcversion");
        args.add(options.getVersionID().getVersion());
        String build = options.getVersionID().getBuild();
        if (build != null) {
            log.info("Adding -loader " + build);
            args.add("-loader");
            args.add(build);
        }

        args.add("-dir");
        String dir = options.getDir();
        if (dir == null) {
            dir = ctx.getMcFiles().getBase().toPath().toAbsolutePath().toString();
        }

        args.add(dir);
        return args;
    }

}
