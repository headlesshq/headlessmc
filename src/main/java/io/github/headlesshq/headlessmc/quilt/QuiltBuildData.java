package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * JSON object from
 * <a href=https://meta.quiltmc.org/v3/versions/loader>https://meta.quiltmc.org/v3/versions/loader</a>:
 * <pre>{@code
 * {
 *     "separator": ".",
 *     "build": 2,
 *     "maven": "org.quiltmc:quilt-loader:0.29.2",
 *     "version": "0.29.2"
 * }
 * }</pre>
 *
 * @param separator always dot.
 * @param build     patch version.
 * @param maven     maven coordinates.
 * @param version   the loader version, betas look like {@code 0.31.0-beta.4}.
 */
@RegisterForReflection
record QuiltBuildData(String separator, int build, String maven, String version) implements ReflectionRegistered {

}
