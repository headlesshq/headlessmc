package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.version.id.VersionID;
import jakarta.enterprise.context.Dependent;
import lombok.Setter;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.Callable;

@Setter
@Dependent
@CommandLine.Command(
        name = "download",
        aliases = "install",
        description = "Downloads a Mc version.",
        mixinStandardHelpOptions = true
)
public class DownloadCommand implements Callable<DownloadedVersion> {
    @CommandLine.Parameters
    private VersionID versionID;

    @Override
    public DownloadedVersion call() throws IOException {



        return null;
    }

}
