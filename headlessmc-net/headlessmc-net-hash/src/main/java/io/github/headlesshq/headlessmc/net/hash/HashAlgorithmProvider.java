package io.github.headlesshq.headlessmc.net.hash;

import java.security.MessageDigest;
import java.util.Optional;

/**
 * Provides a {@link MessageDigest} for a given name.
 * Essentially a wrapper over {@link MessageDigest#getInstance(String)},
 * but with the option to be more lenient than the
 * <a href=https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html>
 * Java Security Standard Algorithm Names Specification
 * </a>.
 * This is especially important as we consume libraries and APIs,
 * that do not follow the Java Algorithms Names Specification.
 */
public interface HashAlgorithmProvider {
    /**
     * Finds a {@link MessageDigest} for the given algorithm name.
     * If no such algorithm can be found, no exception is thrown,
     * but an empty optional is returned.
     *
     * @param name the name of the algorithm.
     * @return a message digest for the algorithm, or an empty optional.
     */
    Optional<MessageDigest> getAlgorithm(String name);

}
