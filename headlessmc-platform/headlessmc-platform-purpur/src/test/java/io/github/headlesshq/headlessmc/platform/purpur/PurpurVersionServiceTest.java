package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.purpur.api.BuildResponse;
import io.github.headlesshq.headlessmc.platform.purpur.api.ProjectResponse;
import io.github.headlesshq.headlessmc.platform.purpur.api.PurpurAPI;
import io.github.headlesshq.headlessmc.platform.purpur.api.VersionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.*;

class PurpurVersionServiceTest {
    /** A {@link PurpurAPI} over {@link #buildsByVersion}. */
    private class TestApi implements PurpurAPI {
        @Override
        public ProjectResponse getProject(String project) {
            return new ProjectResponse(project, new ArrayList<>(buildsByVersion.keySet()));
        }

        @Override
        public VersionResponse getVersion(String project, String version) {
            List<String> builds = buildsByVersion.get(version);
            return new VersionResponse(version, new VersionResponse.Builds(builds.getLast(), builds));
        }

        @Override
        public BuildResponse getBuild(String project, String version, String build) {
            return new BuildResponse(project, version, build, "md5-" + build);
        }
    }

    private final Map<String, List<String>> buildsByVersion = new LinkedHashMap<>();

    private PurpurVersionService service;

    @BeforeEach
    void setup() {
        buildsByVersion.put("1.21.1", List.of("2327", "2328", "2329"));
        service = new PurpurVersionService(new TestApi());
    }

    private VanillaVersion vanilla(String name) {
        return new FakeVanillaPlatform(name).getVersionService().getVersion(name).orElseThrow();
    }

    @Test
    void platformNameIsPurpur() {
        assertEquals(Purpur.PLATFORM_NAME, service.getPlatformName());
    }

    @Test
    void listsAllVersionsLatestBuildFirst() {
        SequencedSet<BoundPlatformVersion> versions = service.getVersions();

        assertEquals(List.of("2329", "2328", "2327"), versions.stream().map(PlatformVersion::getName).toList());
        assertEquals("1.21.1", versions.getFirst().getVanillaVersion());
    }

    @Test
    void listsBuildsLatestFirst() {
        SequencedSet<BoundPlatformVersion> builds = service.getBuilds(vanilla("1.21.1"));

        assertEquals(List.of("2329", "2328", "2327"), builds.stream().map(PlatformVersion::getName).toList());
        assertEquals("1.21.1", builds.getFirst().getVanillaVersion());
    }

    @Test
    void resolvesLatestBuild() {
        BoundPlatformVersion latest = service.getLatestBuild(vanilla("1.21.1")).orElseThrow();

        assertEquals("2329", latest.getName());
        assertEquals("1.21.1", latest.getVanillaVersion());
    }

    @Test
    void resolvesSpecificBuild() {
        assertEquals("2328", service.getBuild(vanilla("1.21.1"), "2328").orElseThrow().getName());
        assertTrue(service.getBuild(vanilla("1.21.1"), "9999").isEmpty());
    }

    @Test
    void sortsBuildsDescending() {
        List<PlatformVersion> builds = new ArrayList<>(service.getBuilds(vanilla("1.21.1")));

        assertEquals(
            List.of("2329", "2328", "2327"),
            service.sort(builds).stream().map(PlatformVersion::getName).toList()
        );
    }

    @Test
    void sortRejectsForeignVersions() {
        FakePlatform paper = FakePlatform.create("paper", new String[]{"1.21.1", "130", "129"});
        List<PlatformVersion> versions = new ArrayList<>(paper.versions());

        assertThrows(IllegalArgumentException.class, () -> service.sort(versions));
    }

    @Test
    void buildResponseBuildsItsDownloadUrl() {
        BuildResponse response = new BuildResponse("purpur", "1.21.1", "2329", "md5");

        assertEquals(
            URI.create("https://api.purpurmc.org/v2/purpur/1.21.1/2329/download"),
            response.getDownloadUrl(PurpurAPI.V2_URL)
        );
    }

    @Test
    void serverFinderUsesTheDefaultJar() {
        assertEquals(
            java.nio.file.Path.of("srv", "server.jar"),
            new PurpurServerFinder().findExecutable(java.nio.file.Path.of("srv"))
        );
    }

}
