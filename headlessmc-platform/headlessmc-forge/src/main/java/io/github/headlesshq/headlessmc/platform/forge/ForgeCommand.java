package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import io.github.headlesshq.headlessmc.version.id.VersionID;
import io.github.headlesshq.headlessmc.version.picocli.VersionIDConverter;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Setter
@Dependent
@CommandLine.Command(
    name = "forge",
    description = "Installs forge for a version.",
    mixinStandardHelpOptions = true
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ForgeCommand implements Callable<Path> {
    private final @Fabric PlatformDownloader platformDownloader;

    @CommandLine.Parameters(converter = VersionIDConverter.class, paramLabel = "<version>") // TODO
    private @Nullable VersionID versionID;

    @CommandLine.Option(names = {"--server"}, description = "Installs the fabric server.")
    private boolean server = false;

    @CommandLine.Option(names = {"--in-memory"}, description = "Runs the Fabric Installer in the same JVM.")
    private boolean inMemory = false;

    @CommandLine.Option(names = {"--build", "--uid"}, description = "The version of the Fabric loader to use.")
    private @Nullable String build;

    @CommandLine.Option(names = {"--java"}, description = "The version of Java to run the installer with.")
    private @Nullable Integer java;

    // TODO: list/array
    @CommandLine.Option(names = {"--jvm"}, description = "JVM Arguments to run the installer with.")
    private @Nullable String jvm;

    @CommandLine.Option(names = {"--dir"}, description = "Directory argument for the Fabric installer.")
    private @Nullable String dir;

    @CommandLine.Option(names = {"--url"}, description = "URL to download the installer from.")
    private @Nullable String url;

    @Override
    public Path call() throws IOException {
        VersionID versionID = this.versionID;
        if (versionID == null) {
            throw new IOException("Specify a version!");
        }

        if (server) {
            versionID = versionID.withServer(true);
        }

        if (build != null) {
            versionID = versionID.withPlatform("fabric").withBuild(build);
        }

        ForgeDownloadOptions options = new ForgeDownloadOptions(
            versionID,
            inMemory,
            null, // TODO: !
            null,
            java,
            new String[] { jvm }, // TODO: fix!
            dir,
            url == null ? null : URI.create(url)
        );

        return platformDownloader.download(options);
    }

}
