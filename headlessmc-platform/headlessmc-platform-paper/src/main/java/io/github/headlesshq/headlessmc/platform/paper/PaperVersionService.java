package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.paper.api.PaperAPI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of a {@link VersionService} for the {@link Paper} platform.
 * Uses the {@link PaperAPI} to get versions and builds.
 *
 * @see <a href=https://docs.papermc.io/misc/downloads-service/>
 * https://docs.papermc.io/misc/downloads-service/
 * </a>
 * @see <a href=https://api.papermc.io/v2/projects/paper/versions/1.12.2/>
 * https://api.papermc.io/v2/projects/paper/versions/1.12.2/
 * </a>
 */
@Paper
@ApplicationScoped
@RequiredArgsConstructor // TODO: cache?
public class PaperVersionService extends AbstractVersionService implements VersionService {
    @Getter
    private final String platformName;
    private final String projectName;
    private final PaperAPI api;

    @Inject
    public PaperVersionService(@RestClient PaperAPI api) {
        this(Paper.PLATFORM_NAME, PaperAPI.PAPER_PROJECT, api);
    }

    @Override
    public SequencedSet<BoundPlatformVersion> getVersions() {
        return api.getVersions(projectName).versions()
            .stream()
            .flatMap(
                response -> response.builds()
                    .stream()
                    .sorted(Comparator.comparingInt(build -> -build)) // largest build is newest, first
                    .map(build -> createVersion(String.valueOf(build), response.version().id()))
            ).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public SequencedSet<BoundPlatformVersion> getBuilds(VanillaVersion version) {
        return api.getBuilds(projectName, version.getName(), null)
            .stream()
            .sorted()
            .map(build -> createVersion(String.valueOf(build.id()), version.getName()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Optional<BoundPlatformVersion> getLatestBuild(VanillaVersion version) {
        try {
            return Optional.of(api.getLatest(projectName, version.getName()))
                .map(response -> createVersion(String.valueOf(response.id()), version.getName()));
        } catch (ApiException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<BoundPlatformVersion> getBuild(VanillaVersion version, String build) {
        try {
            return Optional.of(api.getBuild(projectName, version.getName(), Integer.parseInt(build)))
                .map(response -> createVersion(String.valueOf(response.id()), version.getName()));
        } catch (NumberFormatException | ApiException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public SequencedSet<PlatformVersion> sort(Collection<PlatformVersion> versions) throws IllegalArgumentException {
        return sortByBuildAsInt(getPlatformName(), versions);
    }

    public static SequencedSet<PlatformVersion> sortByBuildAsInt(
        String platformName,
        Collection<PlatformVersion> versions
    ) throws IllegalArgumentException {
        return versions.stream()
                .peek(version -> {
                    if (!platformName.equals(version.getPlatformName())) {
                        throw new IllegalArgumentException("Versions to sort contained other version: " + version + " in " + versions);
                    }
                }).sorted(
                    Comparator.comparingInt((PlatformVersion version) -> {
                        try {
                            return Integer.parseInt(version.getName());
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("PlatformVersion was not an integer: " + version + " in " + versions);
                        }
                    }).reversed() // highest number first
                ).collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
