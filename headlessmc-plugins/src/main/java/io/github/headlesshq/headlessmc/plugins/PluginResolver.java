package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.plugins.version.Version;
import io.github.headlesshq.headlessmc.plugins.version.VersionRange;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides which plugins can be loaded together and which versions of their nested libraries to use.
 * <p>
 * Libraries are resolved like NeoForge JarJar resolves them: every plugin that ships a library
 * declares the range of versions it is compatible with (by default {@code [version, nextMajor)}).
 * The highest shipped version contained in all ranges is used. If there is no such version, every plugin
 * shipping the library is rejected. HeadlessMc's own libraries ({@link CoreLibraries}) cannot be replaced,
 * a plugin shipping one is rejected if the HeadlessMc version is not contained in its range.
 * <p>
 * A rejected plugin may cause other plugins to be rejected, because they depend on it,
 * so resolution repeats until the set of plugins does not change anymore.
 */
@ApplicationScoped
public class PluginResolver {
    private final CoreLibraries coreLibraries;
    private final Version headlessMcVersion;

    @Inject
    public PluginResolver(CoreLibraries coreLibraries) {
        this(coreLibraries, HeadlessMc.VERSION);
    }

    public PluginResolver(CoreLibraries coreLibraries, String headlessMcVersion) {
        this.coreLibraries = coreLibraries;
        this.headlessMcVersion = Version.parse(headlessMcVersion);
    }

    /**
     * Resolves the given plugins.
     *
     * @param files the plugin jars, as returned by {@link PluginScanner#scan}.
     * @return the plugins and libraries that can be loaded, and the problems of the others.
     */
    public PluginResolution resolve(List<PluginFile> files) {
        List<PluginResolution.Problem> problems = new ArrayList<>();
        List<PluginFile> plugins = new ArrayList<>();
        for (PluginFile file : files) {
            String error = file.error();
            if (error != null || file.manifest() == null) {
                problems.add(new PluginResolution.Problem(file, error == null ? "no manifest" : error));
            } else {
                plugins.add(file);
            }
        }

        rejectDuplicateIds(plugins, problems);
        while (true) {
            Set<PluginFile> rejected = new LinkedHashSet<>();
            for (PluginFile plugin : plugins) {
                checkPlugin(plugin, plugins, rejected, problems);
            }

            List<PluginResolution.Library> libraries = new ArrayList<>();
            if (rejected.isEmpty()) {
                resolveLibraries(plugins, libraries, rejected, problems);
            }

            if (rejected.isEmpty()) {
                return new PluginResolution(List.copyOf(plugins), List.copyOf(libraries), List.copyOf(problems));
            }

            plugins.removeAll(rejected);
        }
    }

    private void rejectDuplicateIds(List<PluginFile> plugins, List<PluginResolution.Problem> problems) {
        Map<String, List<PluginFile>> byId = plugins.stream()
            .collect(Collectors.groupingBy(file -> manifest(file).id(), LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<PluginFile>> entry : byId.entrySet()) {
            if (entry.getValue().size() > 1) {
                String names = entry.getValue().stream().map(PluginFile::fileName).collect(Collectors.joining(", "));
                for (PluginFile file : entry.getValue()) {
                    problems.add(new PluginResolution.Problem(file, "plugin id '" + entry.getKey() + "' is used by multiple jars: " + names));
                }

                plugins.removeAll(entry.getValue());
            }
        }
    }

    private void checkPlugin(
        PluginFile plugin,
        List<PluginFile> plugins,
        Set<PluginFile> rejected,
        List<PluginResolution.Problem> problems
    ) {
        PluginManifest manifest = manifest(plugin);
        try {
            VersionRange range = VersionRange.parse(manifest.headlessmc());
            if (!range.contains(headlessMcVersion)) {
                reject(plugin, "requires HeadlessMc " + range + ", but this is " + headlessMcVersion, rejected, problems);
                return;
            }
        } catch (IllegalArgumentException e) {
            reject(plugin, "invalid headlessmc version range: " + e.getMessage(), rejected, problems);
            return;
        }

        for (PluginManifest.Dependency dependency : manifest.dependencyList()) {
            Optional<PluginFile> other = plugins.stream()
                .filter(file -> manifest(file).id().equals(dependency.id()))
                .findFirst();
            if (other.isEmpty()) {
                if (!dependency.optional()) {
                    reject(plugin, "requires plugin '" + dependency.id() + "' " + dependency.versions() + ", which is not installed or cannot be loaded", rejected, problems);
                    return;
                }

                continue;
            }

            try {
                VersionRange range = VersionRange.parse(dependency.versions() == null ? "*" : dependency.versions());
                String otherVersion = manifest(other.get()).version();
                if (!range.contains(Version.parse(otherVersion))) {
                    reject(plugin, "requires plugin '" + dependency.id() + "' " + range + ", but " + otherVersion + " is installed", rejected, problems);
                    return;
                }
            } catch (IllegalArgumentException e) {
                reject(plugin, "invalid version range for dependency '" + dependency.id() + "': " + e.getMessage(), rejected, problems);
                return;
            }
        }
    }

    private void resolveLibraries(
        List<PluginFile> plugins,
        List<PluginResolution.Library> libraries,
        Set<PluginFile> rejected,
        List<PluginResolution.Problem> problems
    ) {
        Map<String, List<Candidate>> byModule = new LinkedHashMap<>();
        for (PluginFile plugin : plugins) {
            for (PluginManifest.Library library : manifest(plugin).libraryList()) {
                Candidate candidate;
                try {
                    Version version = Version.parse(library.version());
                    VersionRange range = library.versions() == null
                        ? VersionRange.compatible(version)
                        : VersionRange.parse(library.versions());
                    candidate = new Candidate(plugin, library, version, range);
                } catch (IllegalArgumentException e) {
                    reject(plugin, "invalid version of library " + library.module() + ": " + e.getMessage(), rejected, problems);
                    continue;
                }

                byModule.computeIfAbsent(library.module(), k -> new ArrayList<>()).add(candidate);
            }
        }

        for (Map.Entry<String, List<Candidate>> entry : byModule.entrySet()) {
            String module = entry.getKey();
            List<Candidate> candidates = entry.getValue();
            Optional<Version> coreVersion = coreLibraries.get(module);
            if (coreVersion.isPresent()) {
                for (Candidate candidate : candidates) {
                    if (!candidate.range().contains(coreVersion.get())) {
                        reject(candidate.plugin(), "requires library " + module + " " + candidate.range()
                            + ", but HeadlessMc ships " + coreVersion.get() + ", which cannot be replaced", rejected, problems);
                    }
                }

                continue;
            }

            Optional<VersionRange> intersection = Optional.of(VersionRange.ANY);
            for (Candidate candidate : candidates) {
                intersection = intersection.flatMap(range -> range.intersect(candidate.range()));
            }

            Optional<Candidate> selected = intersection.flatMap(range -> candidates.stream()
                .filter(candidate -> range.contains(candidate.version()))
                .max(Comparator.comparing(Candidate::version)));
            if (selected.isEmpty()) {
                String requirements = candidates.stream()
                    .map(candidate -> manifest(candidate.plugin()).id() + " ships " + candidate.version() + " and requires " + candidate.range())
                    .collect(Collectors.joining(", "));
                for (Candidate candidate : candidates) {
                    reject(candidate.plugin(), "conflicting versions of library " + module + ": " + requirements, rejected, problems);
                }

                continue;
            }

            List<String> requestedBy = candidates.stream().map(candidate -> manifest(candidate.plugin()).id()).distinct().toList();
            libraries.add(new PluginResolution.Library(selected.get().library(), selected.get().plugin(), requestedBy));
        }
    }

    private static void reject(PluginFile plugin, String message, Set<PluginFile> rejected, List<PluginResolution.Problem> problems) {
        rejected.add(plugin);
        problems.add(new PluginResolution.Problem(plugin, message));
    }

    @SuppressWarnings("DataFlowIssue") // only called for files with a manifest
    private static PluginManifest manifest(PluginFile file) {
        return file.manifest();
    }

    private record Candidate(PluginFile plugin, PluginManifest.Library library, Version version, VersionRange range) {

    }

}
