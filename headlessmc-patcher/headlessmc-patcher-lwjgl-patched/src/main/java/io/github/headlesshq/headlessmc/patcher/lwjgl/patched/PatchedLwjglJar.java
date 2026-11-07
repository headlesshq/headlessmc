package io.github.headlesshq.headlessmc.patcher.lwjgl.patched;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.net.URI;

/**
 * A jar of a headlessmc-lwjgl GitHub release, as locked in {@link PatchedLwjglLock}.
 *
 * @param fileName the name of the release asset, e.g. {@code headlessmc-lwjgl3-glfw-0.1.0.jar}.
 * @param size the size of the jar in bytes.
 * @param sha256 the SHA-256 checksum of the jar.
 * @param url the URL to download the jar from.
 */
@RegisterForReflection
record PatchedLwjglJar(
    String fileName,
    long size,
    String sha256,
    String url
) implements ReflectionRegistered {

    public URI getURL() {
        return URI.create(url);
    }

}
