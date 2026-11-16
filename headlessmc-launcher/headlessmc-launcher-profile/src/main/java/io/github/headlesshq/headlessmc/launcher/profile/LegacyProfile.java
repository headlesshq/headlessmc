package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * A profile in the formats 0 and 1, see {@link Profile#PROFILE_VERSION}.
 * These profiles could only launch a version, which was part of the profile itself.
 * It is only read, to convert it into a {@link Profile} with {@link #migrate()}.
 * Both formats differ only in how the path is written, which the json parser handles.
 * This is public for everything else that stores profiles and has to read what it stored before format 2,
 * like backups.
 *
 * @param version        the version that has been specified.
 * @param currentVersion the version that is launched, nothing ever made it differ from {@code version}.
 */
@RegisterForReflection
public record LegacyProfile(
    String name,
    VersionArg version,
    @Nullable VersionArg currentVersion,
    Path path,
    @Nullable LaunchOptions options,
    @Nullable List<String> patchers,
    @Nullable SequencedMap<String, @Nullable String> systemProperties,
    @Nullable List<String> vmArgs,
    @Nullable List<String> gameArgs,
    @Nullable Integer javaVersion,
    @Nullable EulaStatus eulaStatus,
    boolean hasDefaultClientJvmArgs,
    int hmcVersion
) implements ReflectionRegistered {
    @SuppressWarnings("ConstantValue")
    public LegacyProfile {
        Objects.requireNonNull(name, "Profile is missing 'name'");
        Objects.requireNonNull(version, "Profile " + name + " is missing 'version'");
        Objects.requireNonNull(path, "Profile " + name + " is missing 'path'");
    }

    /**
     * Converts this profile into the current format.
     * The version becomes a {@value VersionHandle#VERSION} target, or a {@value VersionHandle#SERVER} target
     * if it is a server version, which is not resolved yet.
     * A profile without a side has always been a client, this is the only place where a missing side means client.
     * The format version of the profile is kept, {@link ProfileService#save(Profile)} updates it.
     *
     * @return this profile in the current format.
     */
    @SuppressWarnings("DataFlowIssue") // null is replaced with defaults by the constructor
    public Profile migrate() {
        Side side = version.side().orElse(Side.CLIENT);
        VersionArg specified = version.withSide(side);
        VersionHandle handle = VersionHandle.of(specified);
        if (currentVersion != null) {
            handle = handle.withCurrent(VersionHandle.normalize(currentVersion.withSide(side)));
        }

        return new Profile(
            name,
            Optional.of(side),
            handle.asTarget(),
            path,
            options,
            patchers,
            systemProperties,
            vmArgs,
            gameArgs,
            javaVersion,
            eulaStatus,
            hasDefaultClientJvmArgs,
            hmcVersion
        );
    }

}
