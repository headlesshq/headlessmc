package io.github.headlesshq.headlessmc.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * A {@link VanillaVersionService} over a fixed list of version names,
 * given latest-first (like the real version manifest).
 */
public class FakeVanillaVersionService implements VanillaVersionService {
    private final List<String> namesLatestFirst;

    public FakeVanillaVersionService(String... namesLatestFirst) {
        this.namesLatestFirst = List.of(namesLatestFirst);
    }

    @Override
    public VanillaVersion getLatest() {
        return new VanillaVersion(namesLatestFirst.getFirst());
    }

    @Override
    public boolean hasVersion(String name) {
        return namesLatestFirst.contains(name);
    }

    @Override
    public Optional<VanillaVersion> getVersion(String name) {
        return hasVersion(name) ? Optional.of(new VanillaVersion(name)) : Optional.empty();
    }

    @Override
    public SequencedSet<VanillaVersion> getVersions() {
        SequencedSet<VanillaVersion> result = new LinkedHashSet<>();
        for (String name : namesLatestFirst) {
            result.add(new VanillaVersion(name));
        }

        return result;
    }

    @Override
    public Optional<VanillaVersion> getLatestBuild(VanillaVersion version) {
        return Optional.of(version);
    }

    @Override
    public SequencedSet<VanillaVersion> getBuilds(VanillaVersion version) {
        return new LinkedHashSet<>(List.of(version));
    }

    @Override
    public Optional<? extends PlatformVersion> getBuild(VanillaVersion version, String build) {
        return version.getName().equals(build) ? Optional.of(version) : Optional.empty();
    }

    @Override
    public SequencedSet<PlatformVersion> sort(Collection<PlatformVersion> versions) {
        List<PlatformVersion> sorted = new ArrayList<>(versions);
        for (PlatformVersion version : sorted) {
            if (!namesLatestFirst.contains(version.getName())) {
                throw new IllegalArgumentException("Unknown version " + version);
            }
        }

        sorted.sort((a, b) -> Integer.compare(
            namesLatestFirst.indexOf(a.getName()),
            namesLatestFirst.indexOf(b.getName())
        ));
        return new LinkedHashSet<>(sorted);
    }

}
