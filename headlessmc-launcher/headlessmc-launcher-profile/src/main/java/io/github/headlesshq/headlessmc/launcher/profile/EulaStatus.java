package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Before we launch a server we do not know if it is an older version
 * that does not produce a eula.txt to be accepted before running the server,
 * or if it is a server that produces a eula.txt.
 * This enum allows us to keep track of this information for a server.
 */
@RegisterForReflection
public enum EulaStatus implements ReflectionRegistered {
    /**
     * The server produces a eula.txt on launch.
     */
    EXISTS,
    /**
     * The server does not produce a eula.txt on launch.
     */
    NONE,
    /**
     * We do not know yet it the server produces a eula.txt on launch.
     */
    UNKNOWN
}
