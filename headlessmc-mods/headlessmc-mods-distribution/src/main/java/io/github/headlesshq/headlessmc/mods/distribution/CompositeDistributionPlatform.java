package io.github.headlesshq.headlessmc.mods.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ModDistributionPlatform} that combines multiple {@link ModDistributionPlatform}s.
 * Only {@link ModDistributionPlatform#isEnabled() enabled} platforms are used.
 * Searches query every platform, a failing platform is skipped as long as one other platform succeeds.
 * Downloads try the platforms in order and return the first successful download.
 */
@Slf4j
public class CompositeDistributionPlatform implements ModDistributionPlatform {
    private final String name;
    private final List<ModDistributionPlatform> platforms;

    /**
     * @param name      the name of this platform.
     * @param platforms the platforms to combine, in the order in which downloads are tried.
     */
    public CompositeDistributionPlatform(String name, List<ModDistributionPlatform> platforms) {
        this.name = name;
        this.platforms = List.copyOf(platforms);
    }

    /**
     * @return the enabled platforms combined by this platform.
     */
    public List<ModDistributionPlatform> getPlatforms() {
        return platforms.stream().filter(ModDistributionPlatform::isEnabled).toList();
    }

    @Override
    public List<RemoteMod> search(String query, ModType type) throws HeadlessMcException {
        return flatten(searchByPlatform(query, null, type));
    }

    @Override
    public List<RemoteMod> search(String query, VersionArg version, ModType type) throws HeadlessMcException {
        return flatten(searchByPlatform(query, version, type));
    }

    /**
     * Searches all enabled platforms.
     *
     * @param query   the query to search for.
     * @param version the mc version the mods should be for, or {@code null} to search for all versions.
     * @param type    the type of mods to search for.
     * @return the results of each platform that could be searched, by platform name.
     * @throws HeadlessMcException if no platform could be searched.
     */
    public Map<String, List<RemoteMod>> searchByPlatform(String query, @Nullable VersionArg version, ModType type)
        throws HeadlessMcException {
        Map<String, List<RemoteMod>> results = new LinkedHashMap<>();
        List<ModDistributionException> failures = new ArrayList<>();
        for (ModDistributionPlatform platform : enabledPlatforms()) {
            try {
                List<RemoteMod> result = version == null
                    ? platform.search(query, type)
                    : platform.search(query, version, type);
                results.put(platform.getName(), result);
            } catch (ModDistributionException e) {
                log.warn("Failed to search {}: {}", platform.getName(), e.getMessage());
                log.debug("Failed to search {}", platform.getName(), e);
                failures.add(e);
            }
        }

        if (results.isEmpty()) {
            throw failure("Failed to search for " + query, failures);
        }

        return results;
    }

    @Override
    public Path download(
        Path dir,
        @Nullable Path worldDir,
        String id,
        VersionArg version,
        ModType type
    ) throws HeadlessMcException {
        List<ModDistributionException> failures = new ArrayList<>();
        for (ModDistributionPlatform platform : enabledPlatforms()) {
            try {
                return platform.download(dir, worldDir, id, version, type);
            } catch (ModDistributionException e) {
                log.debug("Failed to download {} from {}", id, platform.getName(), e);
                failures.add(e);
            }
        }

        throw failure("Failed to download " + id + " for version " + version, failures);
    }

    @Override
    public boolean isEnabled() {
        return !getPlatforms().isEmpty();
    }

    @Override
    public String getName() {
        return name;
    }

    private List<ModDistributionPlatform> enabledPlatforms() throws ModDistributionException {
        List<ModDistributionPlatform> enabled = getPlatforms();
        if (enabled.isEmpty()) {
            throw new ModDistributionException("No mod distribution platform of " + name + " is enabled");
        }

        return enabled;
    }

    private ModDistributionException failure(String message, List<ModDistributionException> failures) {
        StringBuilder builder = new StringBuilder(message).append(" on ").append(name).append(':');
        for (ModDistributionException failure : failures) {
            builder.append(System.lineSeparator()).append(" - ").append(failure.getMessage());
        }

        ModDistributionException exception = new ModDistributionException(builder.toString());
        failures.forEach(exception::addSuppressed);
        return exception;
    }

    private static List<RemoteMod> flatten(Map<String, List<RemoteMod>> results) {
        return results.values().stream().flatMap(List::stream).toList();
    }

}
