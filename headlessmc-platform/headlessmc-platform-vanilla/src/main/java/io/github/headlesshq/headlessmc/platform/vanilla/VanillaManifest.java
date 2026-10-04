package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * JSON format for
 * <a href=https://launchermeta.mojang.com/mc/game/version_manifest.json>
 * https://launchermeta.mojang.com/mc/game/version_manifest.json
 * </a>, example:
 * <pre>
 * {@code
 * "versions": [
 *  {
 *      "id": "26.2",
 *      "type": "release",
 *      "url": "...",
 *      "time": "2026-06-23T12:05:18+00:00",
 *      "releaseTime": "2026-06-23T11:57:02+00:00"
 *  },
 *  ...
 * ]
 * }
 * </pre>
 *
 * @param versions a list of mc versions.
 * @see <a href=https://launchermeta.mojang.com/mc/game/version_manifest.json>
 * https://launchermeta.mojang.com/mc/game/version_manifest.json
 * </a>
 */
@RegisterForReflection
record VanillaManifest(List<VanillaManifest.Version> versions) implements ReflectionRegistered {
    @RegisterForReflection
    record Version(String id, String type, String url) implements ReflectionRegistered {}

}
