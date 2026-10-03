package io.github.headlesshq.headlessmc.version;

import io.github.headlesshq.headlessmc.util.maven.Artifact;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * A {@link Version} that manages inheritance ({@link Version#getInheritsFrom()}).
 */
public record ProcessedVersion(List<Version> hierarchy) implements Version {
    /**
     * Constructs a new ProcessedVersion for the given hierarchy.
     * The last element should be the target version and all prior
     * elements the corresponding parent version of the last entry,
     * e.g. [1.12.2, forge-1.12.2].
     *
     * @param hierarchy the hierarchy of the version to process, can't be empty.
     * @throws IllegalArgumentException if the hierarchy is empty.
     * @throws NullPointerException     if the hierarchy is {@code null}.
     */
    public ProcessedVersion {
        if (hierarchy.isEmpty()) {
            throw new IllegalArgumentException("Empty version hierarchy");
        }
    }

    @Override
    public String getId() {
        return hierarchy.getLast().getId();
    }

    @Override
    public @Nullable String getInheritsFrom() {
        return hierarchy.getLast().getInheritsFrom();
    }

    @Override
    public @Nullable String getType() {
        return lastNotNull(Version::getType);
    }

    @Override
    public List<Library> getLibraries() {
        return mergeLibraries();
    }

    @Override
    public String getMainClass() {
        return Objects.requireNonNull(lastNotNull(Version::getMainClass), "Failed to get main-class");
    }

    @Override
    public @Nullable String getMinecraftArguments() {
        return lastNotNull(Version::getMinecraftArguments);
    }

    @Override
    public @Nullable Map<String, List<Argument>> getArguments() {
        Map<String, List<Argument>> result = new HashMap<>();
        boolean anyFound = false;
        for (Version rawVersion : hierarchy) {
            Map<String, List<Argument>> arguments = rawVersion.getArguments();
            if (arguments == null) {
                continue;
            }

            anyFound = true;
            arguments.forEach((key, args) -> result.computeIfAbsent(
                key, ignored -> new ArrayList<>()
            ).addAll(args));
        }

        if (anyFound) {
            return result;
        }

        return null;
    }

    @Override
    public AssetIndex getAssetIndex() {
        return Objects.requireNonNull(lastNotNull(Version::getAssetIndex), "Failed to find Asset Index");
    }

    @Override
    public Map<String, LoggingConfiguration> getLogging() {
        Map<String, LoggingConfiguration> logging = new LinkedHashMap<>();
        for (Version version : hierarchy) {
            Map<String, LoggingConfiguration> versionLogging = version.getLogging();
            if (versionLogging != null) {
                logging.putAll(versionLogging);
            }
        }

        return logging;
    }

    @Override
    public Map<String, Download> getDownloads() {
        Map<String, Download> result = new HashMap<>();
        hierarchy.stream().map(Version::getDownloads).filter(Objects::nonNull).forEach(result::putAll);
        return result;
    }

    @Override
    public JavaVersion getJavaVersion() {
        return Objects.requireNonNull(lastNotNull(Version::getJavaVersion), "Failed to find Java version");
    }

    private <T> @Nullable T lastNotNull(Function<Version, @Nullable T> map) {
        return hierarchy.reversed().stream().map(map).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<Library> mergeLibraries() {
        Map<String, Library> libraryMap = new LinkedHashMap<>();
        for (Version version : hierarchy) {
            for (Library library : version.getLibraries()) {
                Artifact coords = library.getArtifact();
                String versionAgnosticId = coords.group() + ":" + coords.name() + ":" + coords.classifier();
                // TODO: what if overridden library is same, but hash/size are missing?!
                // TODO: what is behaviour if inherited library has more/other native classifiers?
                libraryMap.put(versionAgnosticId, library);
            }
        }

        return new ArrayList<>(libraryMap.values());
    }

}
