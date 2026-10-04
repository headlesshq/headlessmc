package io.github.headlesshq.headlessmc.mods.distribution;

// TODO: cache of remote mods to locally downloaded files etc.

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents a mod that can be downloaded from a
 * {@link ModDistributionPlatform}.
 *
 * @param id          an identifier for the mod.
 * @param name        the name of the mod.
 * @param description describes what the mod does.
 */
@RegisterForReflection
public record RemoteMod(
    String id,
    String name,
    String description
    //List<String> authors // would require additional request to /teams endpoint for modrinth
) implements ReflectionRegistered {

}
