package io.github.headlesshq.headlessmc.launcher.profile;

import org.jspecify.annotations.Nullable;

/**
 * A completion candidate for an argument of a {@link LaunchTarget}, see {@link LaunchTarget#complete(TargetArgs)}.
 *
 * @param value       the value to complete the argument to.
 * @param description an optional description of the value.
 */
public record ArgCandidate(String value, @Nullable String description) {
    public ArgCandidate(String value) {
        this(value, null);
    }

}
