package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.files.cache.View;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class AbstractCachedVersionService<V extends PlatformVersion>
    extends AbstractVersionService
    implements VersionService {

    private final String platformName;

    protected abstract View<SequencedSet<V>> getSetView();
    protected abstract View<Map<String, V>> getMapView();
    protected abstract View<Map<V, Integer>> getIndices();

    @Override
    public SequencedSet<V> getVersions() throws HeadlessMcException {
        return getSetView().get().orElseThrow(() -> new NotFoundException("Failed to fetch versions")); // TODO: exception
    }

    @Override
    public abstract SequencedSet<V> getBuilds(VanillaVersion version) throws HeadlessMcException;

    @Override
    public Optional<V> getLatestBuild(VanillaVersion version) throws HeadlessMcException {
        return getBuilds(version).stream().findFirst();
    }

    @Override
    public Optional<? extends PlatformVersion> getBuild(VanillaVersion version, String build) throws HeadlessMcException {
        return getBuilds(version).stream().filter(v -> v.getName().equals(build)).findFirst();
    }

    // TODO: sort by VanillaVersion if BoundPlatformVersion?!
    @Override
    public SequencedSet<PlatformVersion> sort(Collection<PlatformVersion> versions) throws HeadlessMcException {
        // by using the Cache View stream API we can check locally cached
        // versions first (resource, file) and if we have all versions stored
        // locally we can simply sort them like that without needing a lookup
        //noinspection SuspiciousMethodCalls
        Map<V, Integer> order = getIndices().stream()
            .filter(map -> versions.stream().allMatch(map::containsKey))
            .findFirst()
            .orElseThrow(() -> {
                try {
                    SequencedSet<V> allVersions = getSetView().get().orElseGet(LinkedHashSet::new);
                    //noinspection SuspiciousMethodCalls
                    List<PlatformVersion> notIn = versions.stream()
                        .filter(v -> !allVersions.contains(v))
                        .toList();
                    return new NotFoundException("Failed to find versions " + notIn + ", available: " + allVersions);
                } catch (HeadlessMcException e) {
                    return new NotFoundException("Failed to find versions " + versions, e);
                }
            });

        //noinspection SuspiciousMethodCalls
        var result = new TreeSet<PlatformVersion>(
            Comparator.comparingInt(v -> Objects.requireNonNull(
                order.get(v), "Failed to find Platform version " + v + " in " + order)
            )
        );
        result.addAll(versions);
        return new LinkedHashSet<>(result);
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    // helper methods //

    protected View<Map<String, V>> createMapView(View<SequencedSet<V>> setView) {
        return setView.view((value, previous) -> {
            Map<String, V> result = new HashMap<>();
            for (V version : value) {
                result.put(version.getName(), version);
            }

            // TODO: Check previous
            return result;
        });
    }

    protected View<Map<V, Integer>> createIndices(View<SequencedSet<V>> setView) {
        return setView.view((value, previous) -> {
            Map<V, Integer> indices = new HashMap<>();
            int i = 0;
            for (V version : value) {
                indices.put(version, i++);
            }

            // TODO: Check previous
            return indices;
        });
    }

    protected <B extends BoundPlatformVersion> SequencedSet<B> filter(SequencedSet<B> versions, VanillaVersion bound) {
        return versions.stream()
            .filter(version -> version.getVanillaVersion().equals(bound.getName()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
