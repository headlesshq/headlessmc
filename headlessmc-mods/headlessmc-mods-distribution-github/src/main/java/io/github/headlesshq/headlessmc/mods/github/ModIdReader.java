package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;

import java.nio.file.Path;
import java.util.Set;

/**
 * Reads the ids of the mods contained in a jar file.
 */
@FunctionalInterface
public interface ModIdReader {
    /**
     * Reads the ids of the mods contained in a jar file.
     * This never fails, files that cannot be read contain no mods.
     *
     * @param jar     the jar file to read.
     * @param version the version the jar is installed for, which decides how it is read.
     * @return the ids of the mods contained in the jar.
     */
    Set<String> read(Path jar, VersionArg version);

}
