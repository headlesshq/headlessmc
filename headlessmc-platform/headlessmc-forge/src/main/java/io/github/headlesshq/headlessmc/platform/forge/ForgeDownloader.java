package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.platform.AbstractPlatformDownloader;
import io.github.headlesshq.headlessmc.platform.PlatformDownloadOptions;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import io.github.headlesshq.headlessmc.platform.vanilla.VanillaVersionManager;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ForgeDownloader extends AbstractPlatformDownloader implements PlatformDownloader {
    private final ForgeIndexCache forgeIndices;

    public ForgeDownloader(VanillaVersionManager vanillaVersionManager,
                           DownloadService downloadService,
                           VersionService versionService,
                           ForgeIndexCache forgeIndices) {
        super(vanillaVersionManager, downloadService, versionService);
        this.forgeIndices = forgeIndices;
    }

    @Override
    protected Path download(Version vanilla, PlatformDownloadOptions optionsIn, Path versionJson) throws IOException {
        ForgeDownloadOptions options = ForgeDownloadOptions.from(optionsIn);

        String build = options.getVersionID().getBuild();
        List<ForgeVersion> versions = forgeIndices.get()
                .stream()
                .filter(v -> v.getVersion().equals(options.getVersionID().getVersion()))
                .filter(v -> build == null || build.equalsIgnoreCase(v.getName()))
                .collect(Collectors.toList());

        return null;
    }

    private List<String> getArgs(Path installDir, Path fml, boolean server) {
        List<String> args = new ArrayList<>();
        if (server) {
            args.add("--installServer");
            args.add(installDir.toAbsolutePath().toString());
        } else {
            args.add("--installer");
            args.add(fml.toAbsolutePath().toString());
            args.add("--target");
            args.add(installDir.toAbsolutePath().toString());
        }

        return args;
    }

}
