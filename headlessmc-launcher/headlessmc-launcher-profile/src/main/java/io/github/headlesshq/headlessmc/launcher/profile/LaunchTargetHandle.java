package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

/**
 * What a {@link Profile} launches, as it is stored in the profile.
 * The handle does not know what it means: {@link #name()} selects the {@link LaunchTarget}
 * from the {@link LaunchTargetService}, and only that target understands {@link #data()}.
 *
 * <p>This makes targets pluggable. It also means that a profile whose target is not available,
 * e.g. because the plugin providing it has been removed, can still be read, listed and saved
 * without losing anything: the data it does not understand is written back as it has been read.</p>
 *
 * <p>The data has two shapes. After a profile has been read from its file, it is an untyped json tree
 * (maps, lists and primitives). A {@link LaunchTarget} converts it into its own handle type,
 * see {@link AbstractLaunchTarget#handle(LaunchTargetHandle)}, and may place the typed object in a new handle.
 * Both shapes are written to the same json, but they are not {@link #equals(Object) equal} as objects.
 * {@link ProfileService#save(Profile)} therefore returns the profile as it is read back,
 * so that stored profiles always have the untyped shape.</p>
 *
 * @param name the {@link LaunchTarget#getName() name} of the launch target.
 * @param data the data of the launch target, only the launch target knows its type.
 */
@RegisterForReflection
public record LaunchTargetHandle(
    String name,
    Object data
) implements ReflectionRegistered {
    @SuppressWarnings("ConstantValue")
    public LaunchTargetHandle {
        Objects.requireNonNull(name, "Launch target is missing 'name'");
        Objects.requireNonNull(data, "Launch target " + name + " is missing 'data'");
    }

}
