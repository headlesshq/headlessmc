package io.github.headlesshq.headlessmc.launcher.assets;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * {@link io.github.headlesshq.headlessmc.version.Version.AssetIndex}
 * object that is downloaded to resolve the assets for a mc-version.
 * <pre>
 * {@code
 * {
 *   "map_to_resources": true,
 *   "objects": {
 *     "READ_ME_I_AM_VERY_IMPORTANT": {
 *       "hash": "0d000710b71ca9aafabd8f587768431d0b560b32",
 *       "size": 546
 *     },
 *     "icons/icon_16x16.png": {
 *       "hash": "bdf48ef6b5d0d23bbb02e17d04865216179f510a",
 *       "size": 3665
 *     },
 *     ...
 *   }
 * }}
 * </pre>
 *
 * @param map_to_resources for old legacy versions.
 * @param objects the asset files to download.
 */
@RegisterForReflection
public record AssetIndex(
    Map<String, Download> objects,
    @Nullable Boolean map_to_resources, // pre-1.6
    @Nullable Boolean virtual // legacy
) implements ReflectionRegistered {
    public boolean isVirtual() {
        return virtual != null && virtual;
    }
    
    public boolean mapToResources() {
        return map_to_resources != null && map_to_resources;
    }

    public record Download(String hash, Long size) {}
}
