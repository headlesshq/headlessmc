package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link VersionMatcherService} over a fixed map
 * of version id to matching {@link VersionID}s.
 */
public class FakeVersionMatcherService implements VersionMatcherService {
    private final Map<String, Set<VersionID>> matches = new LinkedHashMap<>();

    public FakeVersionMatcherService withMatch(String versionId, VersionID... ids) {
        matches.computeIfAbsent(versionId, k -> new LinkedHashSet<>()).addAll(List.of(ids));
        return this;
    }

    @Override
    public Set<VersionID> match(Version version, VersionProcessor processor) {
        return matches.getOrDefault(version.getId(), Set.of());
    }

    @Override
    public Optional<MatchResult> match(VersionID id, Collection<Version> versions, VersionProcessor processor) {
        for (Version version : versions) {
            for (VersionID candidate : matches.getOrDefault(version.getId(), Set.of())) {
                if (candidate.isSamePlatformVersion(id)) {
                    return Optional.of(new MatchResult(candidate, version));
                }
            }
        }

        return Optional.empty();
    }

}
