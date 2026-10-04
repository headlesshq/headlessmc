package io.github.headlesshq.headlessmc.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * A {@link VersionService} for a modded platform over a fixed map of
 * vanilla version name to build names (builds given latest-first).
 */
public class FakeVersionService implements VersionService {
    private final String platformName;
    private final Map<String, List<String>> builds = new LinkedHashMap<>();

    public FakeVersionService(String platformName) {
        this.platformName = platformName;
    }

    public FakeVersionService withBuilds(String vanillaVersion, String... buildsLatestFirst) {
        builds.put(vanillaVersion, List.of(buildsLatestFirst));
        return this;
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    @Override
    public SequencedSet<BoundPlatformVersion> getVersions() {
        SequencedSet<BoundPlatformVersion> result = new LinkedHashSet<>();
        builds.forEach((vanilla, names) ->
            names.forEach(name -> result.add(new BoundPlatformVersion(platformName, vanilla, name))));
        return result;
    }

    @Override
    public SequencedSet<BoundPlatformVersion> getBuilds(VanillaVersion version) {
        SequencedSet<BoundPlatformVersion> result = new LinkedHashSet<>();
        for (String name : builds.getOrDefault(version.getName(), List.of())) {
            result.add(new BoundPlatformVersion(platformName, version.getName(), name));
        }

        return result;
    }

    @Override
    public Optional<BoundPlatformVersion> getLatestBuild(VanillaVersion version) {
        return getBuilds(version).stream().findFirst();
    }

    @Override
    public Optional<BoundPlatformVersion> getBuild(VanillaVersion version, String build) {
        return getBuilds(version).stream()
            .filter(platformVersion -> platformVersion.getName().equals(build))
            .findFirst();
    }

    @Override
    public SequencedSet<PlatformVersion> sort(Collection<PlatformVersion> versions) {
        List<PlatformVersion> order = new ArrayList<>(getVersions());
        List<PlatformVersion> sorted = new ArrayList<>(versions);
        for (PlatformVersion version : sorted) {
            if (!order.contains(version)) {
                throw new IllegalArgumentException("Unknown version " + version);
            }
        }

        sorted.sort((a, b) -> Integer.compare(order.indexOf(a), order.indexOf(b)));
        return new LinkedHashSet<>(sorted);
    }

}
