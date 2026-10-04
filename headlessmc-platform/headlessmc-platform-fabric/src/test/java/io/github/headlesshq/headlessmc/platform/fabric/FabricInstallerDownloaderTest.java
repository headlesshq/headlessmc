package io.github.headlesshq.headlessmc.platform.fabric;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FabricInstallerDownloaderTest {
    @Inject
    FabricInstallerDownloader installerDownloader;
    @Inject
    InstallerArtifactService installerArtifactService;

    @Test
    @Disabled("Was used for hash issues")
    public void testInstallerDownloader() {
        installerDownloader.download(installerArtifactService.getInstaller());
    }

}
