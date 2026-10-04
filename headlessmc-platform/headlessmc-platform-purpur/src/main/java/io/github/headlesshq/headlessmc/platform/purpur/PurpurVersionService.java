package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.platform.paper.Paper;
import io.github.headlesshq.headlessmc.platform.paper.PaperVersionService;
import io.github.headlesshq.headlessmc.platform.paper.api.PaperAPI;
import io.github.headlesshq.headlessmc.platform.purpur.api.PurpurAPI;
import io.github.headlesshq.headlessmc.platform.purpur.api.VersionResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;
import java.util.stream.Collectors;

// TODO: cache?
/**
 * Implementation of a {@link VersionService} for the {@link Purpur} platform.
 * Uses the {@link PurpurAPI} to get versions and builds.
 */
@Purpur
@ApplicationScoped
@RequiredArgsConstructor
public class PurpurVersionService extends AbstractVersionService implements VersionService {
    @Getter
    private final String platformName;
    private final String projectName;
    private final PurpurAPI api;

    @Inject
    public PurpurVersionService(@RestClient PurpurAPI api) {
        this(Purpur.PLATFORM_NAME, PurpurAPI.PURPUR_PROJECT, api);
    }

    /**
     * WARNING: This call is quite expensive!
     * This should generally be okay, as this is only needed for
     * {@link VersionMatcher},
     * and the Purpur Platform does not implement one.
     *
     * @return all versions for the Purpur Platform.
     */
    // TODO: caching, in-memory for an hour or something
    @Override
    public SequencedSet<BoundPlatformVersion> getVersions() throws UncheckedInterruptedException {
        SequencedSet<BoundPlatformVersion> result = new LinkedHashSet<>();
        try {
            for (String version : api.getProject(projectName).versions()) {
                Thread.sleep(500); // sleep before getting builds
                List<String> builds = api.getVersion(projectName, version).builds().all()
                    .reversed(); // API returns builds in order, latest last

                for (String build : builds) {
                    result.add(createVersion(build, version));
                }

                Thread.sleep(500); // sleep before next API call
            }
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }

        return result;
    }

    @Override
    public SequencedSet<BoundPlatformVersion> getBuilds(VanillaVersion version) {
        return api.getVersion(projectName, version.getName())
            .builds()
            .all()
            .stream()
            .map(build -> createVersion(build, version.getName()))
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .reversed(); // API returns builds in order, latest last // TODO: check paper
    }

    @Override
    public Optional<BoundPlatformVersion> getLatestBuild(VanillaVersion version) {
        return Optional.of(api.getVersion(projectName, version.getName()))
            .map(VersionResponse::builds)
            .map(VersionResponse.Builds::latest)
            .map(build -> createVersion(build, version.getName()));
    }

    @Override
    public Optional<BoundPlatformVersion> getBuild(VanillaVersion version, String build) {
        return api.getVersion(projectName, version.getName())
            .builds()
            .all()
            .stream()
            .filter(purpurBuild -> purpurBuild.equals(build))
            .map(purpurBuild -> createVersion(purpurBuild, version.getName()))
            .findFirst();
    }

    @Override
    public SequencedSet<PlatformVersion> sort(Collection<PlatformVersion> versions) throws IllegalArgumentException {
        return PaperVersionService.sortByBuildAsInt(getPlatformName(), versions);
    }

}
