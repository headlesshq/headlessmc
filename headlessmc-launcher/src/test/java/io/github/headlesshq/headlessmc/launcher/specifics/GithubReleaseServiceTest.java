package io.github.headlesshq.headlessmc.launcher.specifics;

import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GithubReleaseServiceTest {
    @Test
    public void testGithubRelease() throws IOException {
        GithubReleaseService releaseService = new GithubReleaseService(new DownloadService());
        GithubReleaseService.GithubRelease releases = releaseService.getGithubRelease("headlesshq", "hmc-specifics");
        // TOOD: test repository
    }

}
