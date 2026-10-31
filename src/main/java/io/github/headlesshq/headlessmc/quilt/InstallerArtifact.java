package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.net.URI;

/**
 * The Quilt installer, generated into {@value #RESOURCE} by build.gradle.
 */
@RegisterForReflection
record InstallerArtifact(
    String group,
    String name,
    String version,
    String fileName,
    long size,
    String sha256,
    String repository
) implements ReflectionRegistered {
    static final String RESOURCE = "quilt/quilt-installer.json";

    URI getURL() {
        return URI.create("%s/%s/%s/%s/%s".formatted(
            repository,
            String.join("/", group.split("\\.")),
            name,
            version,
            fileName
        ));
    }

}
