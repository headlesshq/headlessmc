package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.platform.PlatformDownloadOptions;
import io.github.headlesshq.headlessmc.version.id.VersionID;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class ForgeDownloadOptions implements PlatformDownloadOptions {
    private final VersionID versionID;
    private final boolean inMemory;
    private final PathProvider pathProvider;
    private final PathProvider vanillaPathProvider;
    private final @Nullable Integer java;
    private final String @Nullable [] jvm;
    private final @Nullable String dir;
    private final @Nullable URI url;

    @Override
    public PathProvider getPathProvider() {
        return dir == null ? pathProvider : v -> Paths.get(dir);
    }

    public List<String> getJvmArgs() {
        return jvm == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(jvm));
    }

    public static ForgeDownloadOptions from(PlatformDownloadOptions options) {
        if (options instanceof ForgeDownloadOptions) {
            return (ForgeDownloadOptions) options;
        }

        return new ForgeDownloadOptions(
            options.getVersionID(),
            options.isInMemory(),
            options.getPathProvider(),
            options.getVanillaPathProvider(),
            null, null, null, null
        );
    }

}
