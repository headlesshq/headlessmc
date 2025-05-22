package io.github.headlesshq.headlessmc.launcher.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

/**
 * Information about a {@link StepFullJavaSession} that is required to launch the game.
 */
@Data
@AllArgsConstructor
public class LaunchAccount implements HasName {
    private final String type;
    private final String name;
    private final String id;
    private final String token;
    private final String xuid;

}
