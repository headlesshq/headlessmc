package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.platform.paper.api.BuildResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.Channel;
import io.github.headlesshq.headlessmc.platform.paper.api.PaperAPI;
import io.github.headlesshq.headlessmc.platform.paper.api.ProjectResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.VersionResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.VersionsResponse;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperJvmArgsServiceTest {
    private static final List<String> FLAGS = List.of("-XX:+UseG1GC", "-XX:+AlwaysPreTouch");

    private static PaperJvmArgsService service(Function<String, VersionResponse> versions) {
        return new PaperJvmArgsService(new PaperAPI() {
            @Override
            public ProjectResponse getProject(String project) {
                throw new UnsupportedOperationException();
            }

            @Override
            public VersionsResponse getVersions(String project) {
                throw new UnsupportedOperationException();
            }

            @Override
            public VersionResponse getVersion(String project, String version) {
                assertEquals(PaperAPI.PAPER_PROJECT, project);
                return versions.apply(version);
            }

            @Override
            public BuildResponse getBuild(String project, String version, Integer build) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BuildResponse getLatest(String project, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<BuildResponse> getBuilds(String project, String version, List<Channel> channel) {
                throw new UnsupportedOperationException();
            }
        });
    }

    private static VersionResponse version(String version, VersionResponse.Java java) {
        return new VersionResponse(
            new VersionResponse.Version(version, java),
            new VersionResponse.Support("SUPPORTED", null),
            List.of(130)
        );
    }

    @Test
    void returnsTheRecommendedFlags() {
        PaperJvmArgsService service = service(version -> version(version, new VersionResponse.Java(
            new VersionResponse.JavaVersion(21),
            Map.of(PaperJvmArgsService.RECOMMENDED, FLAGS, "other", List.of("-Xmx1G"))
        )));

        assertEquals(FLAGS, service.getRecommendedJvmArgs("1.21.1"));
    }

    @Test
    void returnsNothingIfThereAreNoRecommendations() {
        assertEquals(List.of(), service(version -> version(version, null)).getRecommendedJvmArgs("1.21.1"));
        assertEquals(List.of(), service(version -> version(version, new VersionResponse.Java(
            new VersionResponse.JavaVersion(21), null
        ))).getRecommendedJvmArgs("1.21.1"));
        assertEquals(List.of(), service(version -> version(version, new VersionResponse.Java(
            new VersionResponse.JavaVersion(21), Map.of()
        ))).getRecommendedJvmArgs("1.21.1"));
    }

    @Test
    void returnsNothingIfTheApiFails() {
        assertEquals(List.of(), service(version -> {
            throw new ApiException(Response.status(404).build());
        }).getRecommendedJvmArgs("26.1"));
        assertEquals(List.of(), service(version -> {
            throw new ProcessingException("offline");
        }).getRecommendedJvmArgs("26.1"));
    }

}
