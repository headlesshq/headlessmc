package io.github.headlesshq.headlessmc.launcher.specifics;

import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionSpecificModRepositoryTest {
    @Test
    @Disabled("Requests the GitHub API")
    public void testVersionSpecificModRepository() throws IOException {
        URL url = new URL("https://github.com/headlesshq/hmc-test-repo/releases/download/");
        VersionSpecificModRepository hmcTestRepo = new VersionSpecificModRepository(url, "headlesshq", "hmc-test-repo", "0.2.0", "-release");
        assertEquals("0.1.0", hmcTestRepo.getVersion(new DownloadService()));
        assertEquals("hmc-test-repo-1.21.11-0.1.0-lexforge-release.jar", hmcTestRepo.getFileName(hmcTestRepo.getVersion(new DownloadService()), new VersionInfo("1.21.11", Modlauncher.LEXFORGE)));
    }

}
