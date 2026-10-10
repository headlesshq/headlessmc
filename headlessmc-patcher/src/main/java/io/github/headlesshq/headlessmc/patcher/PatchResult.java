package io.github.headlesshq.headlessmc.patcher;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;

/**
 * Represents the result of patching a java process before it launches:
 * modifying classpath, adding agents and system properties
 *
 * @param files            the files that are on the classpath.
 * @param javaAgents       a list of java agent jars that should run on the JVM.
 * @param systemProperties a map of key=value system properties the JVM should be launched with.
 */
public record PatchResult(
    @Unmodifiable SequencedSet<Path> files,
    @Unmodifiable SequencedSet<Path> javaAgents,
    @Unmodifiable Map<String, @Nullable String> systemProperties
) {
    public PatchResult(
        @Unmodifiable SequencedSet<Path> files,
        @Unmodifiable SequencedSet<Path> javaAgents
    ) {
        this(files, javaAgents, Map.of());
    }

    /**
     * Creates a new PatchResult and adds the given file to its classpath.
     *
     * @param file the file to add.
     * @return a new PatchResult also containing the given file.
     */
    public PatchResult withFile(Path file) {
        SequencedSet<Path> newClasspath = new LinkedHashSet<>(files);
        newClasspath.add(file);
        return new PatchResult(newClasspath, javaAgents, systemProperties);
    }

    /**
     * Creates a new PatchResult and adds the javaAgent file to it.
     *
     * @param file the javaagent to add.
     * @return a new PatchResult also containing the given agent.
     */
    public PatchResult withAgent(Path file) {
        SequencedSet<Path> newAgents = new LinkedHashSet<>(javaAgents);
        newAgents.add(file);
        return new PatchResult(files, newAgents, systemProperties);
    }

    /**
     * Adds the given SystemProperty to this PatchResult.
     *
     * @param key   the name of the SystemProperty.
     * @param value the value of the SystemProperty.
     * @return a new PatchResult with the given system property.
     */
    public PatchResult withSystemProperty(String key, @Nullable String value) {
        Map<String, @Nullable String> newSystemProperties = new LinkedHashMap<>(systemProperties);
        newSystemProperties.put(key, value);
        return new PatchResult(files, javaAgents, Collections.unmodifiableMap(newSystemProperties));
    }

}
