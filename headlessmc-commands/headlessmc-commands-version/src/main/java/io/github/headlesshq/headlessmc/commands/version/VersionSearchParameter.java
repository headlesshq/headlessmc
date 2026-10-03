package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.version.arg.Side;

import java.util.Set;

record VersionSearchParameter(
    Set<Side> sides,
    Set<Platform> platforms,
    Set<VanillaVersion> versions,
    Set<String> other
) {

}
