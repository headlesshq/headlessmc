package io.github.headlesshq.headlessmc.plugins;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * A jar found in the plugins directory.
 *
 * @param path     the jar.
 * @param sha256   the SHA-256 hash of the jar.
 * @param manifest the manifest of the plugin, or {@code null} if it could not be read.
 * @param error    the reason the manifest could not be read, or {@code null}.
 */
public record PluginFile(Path path, String sha256, @Nullable PluginManifest manifest, @Nullable String error) {
    /**
     * @return the file name of the jar.
     */
    public String fileName() {
        return path.getFileName().toString();
    }

}
