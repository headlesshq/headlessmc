package io.github.headlesshq.headlessmc.platform.paper.api;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link PaperAPI} against the real fill.papermc.io api.
 * Disabled because it requires network access.
 */
@QuarkusTest
class PaperAPITest {
    private static final String VERSION = "1.21.5";

    @Inject
    @RestClient
    PaperAPI paperAPI;

    @Test
    @Disabled
    void testGetProject() throws ApiException, ProcessingException {
        ProjectResponse result = paperAPI.getProject(PaperAPI.PAPER_PROJECT);
        assertEquals(PaperAPI.PAPER_PROJECT, result.project().id());
        assertTrue(result.versions().get("1.21").contains(VERSION));
    }

    @Test
    @Disabled
    void testGetVersions() throws ApiException, ProcessingException {
        List<VersionResponse> result = paperAPI.getVersions(PaperAPI.PAPER_PROJECT).versions();
        assertTrue(result.stream().anyMatch(response -> VERSION.equals(response.version().id())));
    }

    @Test
    @Disabled
    void testGetVersion() throws ApiException, ProcessingException {
        VersionResponse result = paperAPI.getVersion(PaperAPI.PAPER_PROJECT, VERSION);
        assertEquals(VERSION, result.version().id());
        assertFalse(result.builds().isEmpty());
    }

    @Test
    @Disabled
    void testGetLatest() throws ApiException, ProcessingException {
        BuildResponse result = paperAPI.getLatest(PaperAPI.PAPER_PROJECT, VERSION);
        BuildResponse.Download download = result.getServerDownload();
        assertNotNull(download.checksums().get("sha256"));
        assertTrue(download.size() > 0);
        assertTrue(download.url().startsWith("https://"));
    }

    @Test
    @Disabled
    void testGetBuild() throws ApiException, ProcessingException {
        BuildResponse latest = paperAPI.getLatest(PaperAPI.PAPER_PROJECT, VERSION);
        BuildResponse result = paperAPI.getBuild(PaperAPI.PAPER_PROJECT, VERSION, latest.id());
        assertEquals(latest.id(), result.id());
    }

    @Test
    @Disabled
    void testGetBuilds() throws ApiException, ProcessingException {
        List<BuildResponse> all = paperAPI.getBuilds(PaperAPI.PAPER_PROJECT, VERSION, null);
        assertFalse(all.isEmpty());

        // not every version has STABLE builds, 1.21.5 for example only has ALPHA builds
        Channel channel = all.getFirst().getChannel();
        List<BuildResponse> filtered = paperAPI.getBuilds(PaperAPI.PAPER_PROJECT, VERSION, List.of(channel));
        assertFalse(filtered.isEmpty());
        assertTrue(filtered.stream().allMatch(build -> build.getChannel() == channel));
    }

    @Test
    @Disabled
    void testUnknownVersionThrowsApiException() {
        assertThrows(ApiException.class, () -> paperAPI.getVersion(PaperAPI.PAPER_PROJECT, "0.0.0-does-not-exist"));
    }

}
