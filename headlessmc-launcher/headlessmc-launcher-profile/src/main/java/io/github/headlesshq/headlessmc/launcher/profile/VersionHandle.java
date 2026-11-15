package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link LaunchTargetHandle#data() data} of the launch targets that launch a version:
 * the {@value #VERSION} target for clients and the {@value #SERVER} target for servers.
 * They share this type, because both describe what they launch by a {@link VersionArg}.
 *
 * <p>Only {@link #version()} is given by the user. The other two fields are {@code null} until
 * the target has been resolved for the first time, and are then <em>pinned</em>:
 * they only change if the pin is broken (the file is gone), if the profile is updated on request,
 * or if {@link #version()} is edited. A profile for {@code fabric/26.3/latest} therefore keeps launching
 * the same build, instead of whatever is the latest build on that day.</p>
 *
 * @param version       the version that has been specified, e.g. {@code fabric/26.3/latest}.
 *                      Never has a side for {@value #VERSION}, always {@link Side#SERVER} for {@value #SERVER}.
 * @param current       the version the specified version currently resolves to, e.g. {@code fabric/26.3/0.19.5},
 *                      {@code null} if it has not been resolved yet.
 * @param currentTarget the file that is launched: for clients the version.json the version resolves to,
 *                      for servers the executable of the server. {@code null} if it has not been resolved yet.
 *                      With this file a profile can still be launched if its platform is no longer available,
 *                      e.g. because the plugin providing the platform has been removed.
 */
@With
@RegisterForReflection
public record VersionHandle(
    VersionArg version,
    @Nullable VersionArg current,
    @Nullable Path currentTarget
) implements ReflectionRegistered {
    /**
     * The name of the launch target that launches client versions.
     */
    public static final String VERSION = "version";
    /**
     * The name of the launch target that launches server versions.
     */
    public static final String SERVER = "server";

    @SuppressWarnings("ConstantValue")
    public VersionHandle {
        Objects.requireNonNull(version, "Launch target data is missing 'version'");
    }

    /**
     * Creates an unresolved handle for a version, normalizing the side of the version:
     * versions for {@link Side#SERVER} keep their side, all other versions lose it.
     *
     * @param version the version to launch.
     * @return an unresolved handle for the version.
     */
    public static VersionHandle of(VersionArg version) {
        return new VersionHandle(normalize(version), null, null);
    }

    /**
     * @param version the version to normalize.
     * @return the version with {@link Side#SERVER} if it is a server version, and without a side otherwise.
     */
    public static VersionArg normalize(VersionArg version) {
        return version.withSide(isServer(version) ? Side.SERVER : null);
    }

    private static boolean isServer(VersionArg version) {
        return Side.SERVER.equals(version.side().orElse(null));
    }

    /**
     * @return {@code true} if this handle is for the {@value #SERVER} target.
     */
    public boolean isServer() {
        return isServer(version);
    }

    /**
     * @return the name of the launch target this handle is for, {@value #VERSION} or {@value #SERVER}.
     */
    public String targetName() {
        return isServer() ? SERVER : VERSION;
    }

    /**
     * @return a {@link LaunchTargetHandle} with this handle as its data.
     */
    public LaunchTargetHandle asTarget() {
        return new LaunchTargetHandle(targetName(), this);
    }

    /**
     * @return the best known version: {@link #current()} if this handle has been resolved, else {@link #version()}.
     */
    public VersionArg effectiveVersion() {
        return current == null ? version : current;
    }

    /**
     * @return {@link #current()} as an optional.
     */
    public Optional<VersionArg> currentVersion() {
        return Optional.ofNullable(current);
    }

    /**
     * @return a handle for the same version that has to be resolved again.
     */
    public VersionHandle unresolved() {
        return new VersionHandle(version, null, null);
    }

}
