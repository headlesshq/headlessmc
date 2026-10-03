package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.paper.api.BuildResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.Channel;
import io.github.headlesshq.headlessmc.platform.paper.api.PaperAPI;
import io.github.headlesshq.headlessmc.platform.paper.api.ProjectResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.VersionResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.VersionsResponse;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.*;

class PaperVersionServiceTest {
    /** A {@link PaperAPI} over the fields of this test. */
    private class TestApi implements PaperAPI {
        @Override
        public ProjectResponse getProject(String project) {
            throw new UnsupportedOperationException();
        }

        @Override
        public VersionsResponse getVersions(String project) {
            return new VersionsResponse(versions);
        }

        @Override
        public VersionResponse getVersion(String project, String version) {
            return versions.stream()
                .filter(response -> response.version().id().equals(version))
                .findFirst()
                .orElseThrow(() -> new ApiException("Unknown version " + version));
        }

        @Override
        public BuildResponse getBuild(String project, String version, Integer build) {
            return builds.stream()
                .filter(response -> response.id() == build)
                .findFirst()
                .orElseThrow(() -> new ApiException("Unknown build " + build));
        }

        @Override
        public BuildResponse getLatest(String project, String version) {
            return builds.stream()
                .max(java.util.Comparator.comparingInt(BuildResponse::id))
                .orElseThrow(() -> new ApiException("No builds for " + version));
        }

        @Override
        public List<BuildResponse> getBuilds(String project, String version, List<Channel> channel) {
            return builds;
        }
    }

    private final List<VersionResponse> versions = new ArrayList<>();
    private final List<BuildResponse> builds = new ArrayList<>();

    private PaperVersionService service;

    @BeforeEach
    void setup() {
        versions.add(version("1.21.1", 130, 129));
        versions.add(version("1.20.4", 100));
        builds.add(build(129));
        builds.add(build(130));
        service = new PaperVersionService(new TestApi());
    }

    private VersionResponse version(String id, int... buildIds) {
        List<Integer> buildList = new ArrayList<>();
        for (int build : buildIds) {
            buildList.add(build);
        }

        return new VersionResponse(
            new VersionResponse.Version(id, new VersionResponse.Java(new VersionResponse.JavaVersion(21), Map.of())),
            new VersionResponse.Support("SUPPORTED", null),
            buildList
        );
    }

    private BuildResponse build(int id) {
        return new BuildResponse(id, "STABLE", Map.of(
            BuildResponse.DEFAULT_DOWNLOAD,
            new BuildResponse.Download("paper-" + id + ".jar", Map.of("sha256", "hash"), 10L, "https://p/" + id)
        ));
    }

    private VanillaVersion vanilla(String name) {
        FakeVanillaPlatform platform = new FakeVanillaPlatform(name);
        return platform.getVersionService().getVersion(name).orElseThrow();
    }

    @Test
    void platformNameIsPaper() {
        assertEquals(Paper.PLATFORM_NAME, service.getPlatformName());
    }

    @Test
    void listsAllVersionsWithLatestBuildFirst() {
        SequencedSet<BoundPlatformVersion> result = service.getVersions();

        assertEquals(
            List.of("130", "129", "100"), result.stream().map(PlatformVersion::getName).toList()
        );
        assertEquals("1.21.1", result.getFirst().getVanillaVersion());
    }

    @Test
    void listsBuildsOfAVersion() {
        // BuildResponse sorts descending (latest first)
        assertEquals(
            List.of("130", "129"),
            service.getBuilds(vanilla("1.21.1")).stream().map(PlatformVersion::getName).toList()
        );
    }

    @Test
    void resolvesLatestBuild() {
        assertEquals("130", service.getLatestBuild(vanilla("1.21.1")).orElseThrow().getName());
    }

    @Test
    void latestBuildOfUnknownVersionIsEmpty() {
        builds.clear();
        assertEquals(Optional.empty(), service.getLatestBuild(vanilla("1.21.1")));
    }

    @Test
    void resolvesSpecificBuild() {
        assertEquals("129", service.getBuild(vanilla("1.21.1"), "129").orElseThrow().getName());
    }

    @Test
    void unknownOrMalformedBuildIsEmpty() {
        assertEquals(Optional.empty(), service.getBuild(vanilla("1.21.1"), "999"));
        assertEquals(Optional.empty(), service.getBuild(vanilla("1.21.1"), "not-a-number"));
    }

    @Test
    void sortsBuildsDescending() {
        SequencedSet<PlatformVersion> sorted = service.sort(new ArrayList<>(
            new LinkedHashSet<>(service.getBuilds(vanilla("1.21.1")))
        ));

        assertEquals(List.of("130", "129"), sorted.stream().map(PlatformVersion::getName).toList());
    }

    @Test
    void sortRejectsForeignPlatformVersions() {
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        List<PlatformVersion> versions = new ArrayList<>(fabric.versions());

        assertThrows(IllegalArgumentException.class, () -> service.sort(versions));
    }

    @Test
    void sortRejectsNonNumericBuilds() {
        FakePlatform paper = FakePlatform.create(
            Paper.PLATFORM_NAME, new String[]{"1.21.1", "not-a-number", "also-not-a-number"}
        );
        List<PlatformVersion> versions = new ArrayList<>(paper.versions());

        assertThrows(IllegalArgumentException.class, () -> service.sort(versions));
    }

    @Test
    void serverFinderUsesTheDefaultJar() {
        assertEquals(
            java.nio.file.Path.of("srv", "server.jar"),
            new PaperServerFinder().findExecutable(java.nio.file.Path.of("srv"))
        );
    }

    @Test
    void versionIdsCanBeResolvedAgainstThePlatform() {
        FakePlatform paper = new FakePlatform(Paper.PLATFORM_NAME, service);
        FakePlatformService platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1", "1.20.4"), paper
        );

        assertEquals(
            "130",
            io.github.headlesshq.headlessmc.platform.VersionID
                .resolve(platformService, VersionArg.parse("paper", "1.21.1", "130"))
                .getBuild().orElseThrow().getName()
        );
    }

}
