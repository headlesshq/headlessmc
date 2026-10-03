package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.net.DownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Default
@Vanilla
@ApplicationScoped
class VanillaInstallerImpl implements VanillaInstaller {
    private final VersionJsonService versionJsonService;
    private final DownloadService downloadService;
    private final Cache<VanillaManifest> cache;
    private final McFiles mcFiles;

    @Inject
    VanillaInstallerImpl(
        VersionJsonService versionJsonService,
        DownloadService downloadService,
        @Vanilla Cache<VanillaManifest> cache,
        McFiles mcFiles
    ) {
        this.versionJsonService = versionJsonService;
        this.downloadService = downloadService;
        this.cache = cache;
        this.mcFiles = mcFiles;
    }

    @Override
    public Version installClient(VersionID id, Path mcDir, TypedMap args) throws HeadlessMcException {
        log.info("Installing client {} in {} args: {}", id, mcDir, args);
        return downloadVersion(id.getVersion(), mcDir, args, true);
    }

    @Override
    public Installation installServer(VersionID id, Path dir, TypedMap args) throws HeadlessMcException {
        log.info("Installing server {} in {} args: {}", id, dir, args);
        Version version = getVersion(id.getVersion());

        Version.Download download;
        if (version.getDownloads() == null
            || (download = version.getDownloads().get(Version.DOWNLOAD_SERVER)) == null
            || download.getUrl() == null) {
            throw new FileException("Failed to find server download for version " + version);
        }

        downloadService.download(URI.create(download.getUrl()))
            .sha1(download.getSha1())
            .size(download.getSize())
            .progressBar("Downloading " + id.asArg().withSide(Side.SERVER))
            .toFile(dir.resolve(ServerFinder.DEFAULT_JAR));

        return new Installation(version.requireJavaVersion());
    }

    @Override
    public Version getVersion(VanillaVersion version) {
        return downloadVersion(version, mcFiles.getMcDir(), new TypedMapImpl(), false);
    }

    private Version downloadVersion(
        VanillaVersion version,
        Path mcDir,
        TypedMap args,
        boolean toFile
    ) {
        Path versionFolder = mcDir.resolve("versions");
        Path json = versionFolder.resolve(version.getName()).resolve(version.getName() + ".json");
        if (!args.get(ClientInstaller.FORCE_INSTALL, false) && Files.exists(json)) {
            return versionJsonService.getParser().parse(json);
        }

        return downloadJsonFile(json, version, toFile);
    }

    private Version downloadJsonFile(
        Path json,
        VanillaVersion version,
        boolean toFile
    ) throws HeadlessMcException {
        URI url = cache.get()
            .stream()
            .map(VanillaManifest::versions)
            .flatMap(List::stream)
            .filter(manifestVersion -> version.getName().equals(manifestVersion.id()))
            .map(manifestVersion -> URI.create(manifestVersion.url()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Failed to find version " + version + " in versions " + cache.get()));

        DownloadBuilder builder = downloadService.download(url);
        if (toFile) {
            builder.toFile(json);
            return versionJsonService.getParser().parse(json);
        } else {
            return builder.map(download -> {
               try (InputStreamReader reader = new InputStreamReader(download.getInputStream())) {
                   return versionJsonService.getParser().parse(reader);
               }
            });
        }
    }

}
