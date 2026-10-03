package io.github.headlesshq.headlessmc.java.version;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.Java;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import org.jspecify.annotations.Nullable;

/**
 * Parses the SystemProperty {@code java.specification.version}.
 */
@Default
@ApplicationScoped
public class SpecificationVersionParser {
    public int parseSpecificationVersion(@Nullable String specificationVersion) {
        if (specificationVersion == null) {
            return Java.JAVA_VERSION_UNKNOWN;
        }

        try {
            if (specificationVersion.startsWith("1.")
                || specificationVersion.startsWith("0.")) {
                String[] split = specificationVersion.split("\\.");
                if (split.length <= 1) {
                    throw new HeadlessMcIOException("Failed to parse specification version " + specificationVersion);
                }

                // JAVA_VERSION_0_9
                return specificationVersion.startsWith("0.")
                    ? -Integer.parseInt(split[1])
                    : Integer.parseInt(split[1]);
            }

            return Integer.parseInt(specificationVersion);
        } catch (NumberFormatException e) {
            throw new HeadlessMcIOException("Failed to parse java.specification.version " + specificationVersion);
        }
    }

}
