package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;

/**
 * Base class for {@link LaunchTarget}s, converts the untyped {@link LaunchTargetHandle#data() data}
 * of a handle into the handle type of the target.
 *
 * <p>This class has no fields, because otherwise implementations could not be
 * {@link jakarta.enterprise.context.ApplicationScoped}, see {@link #jsonService()}.</p>
 *
 * @param <H> the type of the data this target stores in its handles.
 */
public abstract class AbstractLaunchTarget<H extends ReflectionRegistered> implements LaunchTarget {
    /**
     * @return the service used to convert the data of handles.
     */
    protected abstract JsonService jsonService();

    /**
     * @return the type of the data this target stores in its handles.
     */
    protected abstract Class<H> handleType();

    /**
     * @param profile a profile with this target.
     * @return the typed data of the target of the profile.
     * @throws LaunchException if the profile has another target, or its data is invalid.
     */
    public H handle(Profile profile) throws LaunchException {
        try {
            return handle(profile.target());
        } catch (LaunchException e) {
            throw new LaunchException("Profile " + profile.name() + " is invalid: " + e.getMessage(), e);
        }
    }

    /**
     * Converts the data of a handle, which is an untyped json tree after a profile has been read.
     *
     * @param handle a handle of this target.
     * @return the typed data of the handle.
     * @throws LaunchException if the handle is for another target, or its data is invalid.
     */
    public H handle(LaunchTargetHandle handle) throws LaunchException {
        if (!getName().equalsIgnoreCase(handle.name())) {
            throw new LaunchException("Launch target " + handle.name() + " is not a " + getName() + " target");
        }

        try {
            return jsonService().convert(handle.data(), handleType());
        } catch (JsonParseException e) {
            Throwable cause = e;
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
            }

            throw new LaunchException(
                "The data of launch target " + getName() + " is invalid: " + cause.getMessage(), e
            );
        }
    }

    /**
     * @param data the data of a handle.
     * @return a handle of this target with the given data.
     */
    public LaunchTargetHandle handle(H data) {
        return new LaunchTargetHandle(getName(), data);
    }

    @Override
    public boolean isSameTarget(Profile stored, ParsedTarget parsed) {
        return handle(stored).equals(handle(parsed.handle()));
    }

}
