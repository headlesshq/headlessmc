package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * JSON object from
 * <a href=https://meta.fabricmc.net/v2/versions/loader>
 * https://meta.fabricmc.net/v2/versions/loader
 * </a>
 * <pre>
 * {@code
 * {
 *     "separator": ".",
 *     "build": 3,
 *     "maven": "net.fabricmc:fabric-loader:0.19.3",
 *     "version": "0.19.3",
 *     "stable": true
 * }
 * }
 * </pre>
 *
 * @param separator always dot.
 * @param build     patch version
 * @param maven     maven coordinates
 * @param version   semantic version
 * @param stable    if version is stable
 */
@RegisterForReflection
record BuildData(
    String separator, int build, String maven, String version, boolean stable
) implements ReflectionRegistered {

}
