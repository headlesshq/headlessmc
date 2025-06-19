package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.commons.DownloadService;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
public class FabricInstaller {
    private static final URI URL = URI.create("https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.3/fabric-installer-1.0.3.jar");
    private static final String SHA1 = "dd1dbed21b72ecbe140644de742a06dcaa71580f";
    private static final long SIZE = 204_454L;

    private static final URI LEGACY = URI.create("https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/1.0.0/fabric-installer-1.0.0.jar");
    private static final String LEGACY_SHA1 = "7e5e61b722662b0864f228a535505f653136e118";
    private static final long LEGACY_SIZE = 172_586L;

    private final String fileName;
    private final URI installerUrl;
    private final @Nullable String sha1;
    private final @Nullable Long size;
    private final boolean custom;

    public Path getFile(Path base, DownloadService downloadService) throws IOException {
        Path installerJar = base.resolve(fileName);
        if (custom || !Files.exists(installerJar)) {
            downloadService.download(installerUrl, installerJar, size, sha1);
        }

        return installerJar;
    }

    public static FabricInstaller legacy() {
        return new FabricInstaller("fabric-installer-legacy-1.0.0.jar", LEGACY, LEGACY_SHA1, LEGACY_SIZE, false);
    }

    public static FabricInstaller defaultInstaller() {
        return new FabricInstaller("fabric-installer-1.0.3.jar", URL, SHA1, SIZE, false);
    }

}
