package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A command whose arguments are handled by a {@link LaunchTarget},
 * e.g. the {@code fabric 1.21.1} in {@code launch fabric 1.21.1}.
 * This gives the {@link LaunchTargetCompletions} what they need to complete these arguments.
 */
public interface LaunchTargetCommand {
    /**
     * @return the name of the launch target given with {@code --target},
     * {@code null} if the launch target is to be detected from the arguments.
     */
    @Nullable String getTarget();

    /**
     * @return the arguments for the launch target, as far as they have been typed.
     */
    @Nullable List<String> getTargetArgs();

    /**
     * @return the sides this command launches.
     */
    default Set<Side> getSides() {
        return Side.BOTH;
    }

    /**
     * @return {@code true} if the first argument of this command can also be the name of a stored profile.
     */
    default boolean acceptsProfileNames() {
        return true;
    }

}
