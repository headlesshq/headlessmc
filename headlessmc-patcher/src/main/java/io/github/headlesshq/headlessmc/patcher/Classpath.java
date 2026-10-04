package io.github.headlesshq.headlessmc.patcher;

import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

/**
 * Represents a Java classpath.
 *
 * @param files the files that are on the classpath.
 */
public record Classpath(
    @Unmodifiable SequencedSet<Path> files,
    @Unmodifiable SequencedSet<Path> javaAgents
) {
    /**
     * Creates a new Classpath object and adds the given file to it.
     *
     * @param file the file to add.
     * @return a new classpath also containing the given file.
     */
    public Classpath withFile(Path file) {
        SequencedSet<Path> newClasspath = new LinkedHashSet<>(files);
        newClasspath.add(file);
        return new Classpath(newClasspath, javaAgents);
    }

    /**
     * Creates a new Classpath object and adds the javaAgent file to it.
     *
     * @param file the javaagent to add.
     * @return a new classpath also containing the given agent.
     */
    public Classpath withAgent(Path file) {
        SequencedSet<Path> newAgents = new LinkedHashSet<>(javaAgents);
        newAgents.add(file);
        return new Classpath(files, newAgents);
    }

}
