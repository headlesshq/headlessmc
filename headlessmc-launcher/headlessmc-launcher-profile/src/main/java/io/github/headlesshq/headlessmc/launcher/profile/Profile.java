package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

@With
@RegisterForReflection
public record Profile(
    String name,
    VersionArg version, // TODO: rename specified Version
    VersionArg currentVersion, // TODO: rename version
    Path path, // TODO: should this be relative?
    LaunchOptions options,
    List<String> patchers,
    SequencedMap<String, @Nullable String> systemProperties,
    List<String> vmArgs,
    @Nullable List<String> gameArgs,
    @Nullable Integer javaVersion,
    // TODO: @Nullable String account,
    EulaStatus eulaStatus,
    boolean hasDefaultClientJvmArgs,
    // TODO: library overrides?
    // mods?, resourcepacks? etc?
    // TODO: config? HeadlessMc config to set? specify java?
    // override template strings?
    // java version?
    int hmcVersion // versionIdentifier to check if we are outdated,
) implements ReflectionRegistered {
    public static final int PROFILE_VERSION = 0;

    public Side side() {
        return version.side().orElse(Side.CLIENT);
    }

    public Profile(String name, VersionArg version, Path path) {
        this(
            name,
            version,
            version,
            path,
            new LaunchOptions(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false
            ),
            List.of(),
            new LinkedHashMap<>(),
            List.of(),
            // if server doesn't display a gui by default, maybe to-do --ui flag in launch command to remove this arg?
            Side.SERVER.equals(version.side().orElse(Side.CLIENT)) ? List.of("nogui") : List.of(),
            null,
            EulaStatus.UNKNOWN,
            false,
            Profile.PROFILE_VERSION
        );
    }

}
