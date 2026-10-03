package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.net.URI;

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

    public URI getURL() {
        return URI.create("%s/%s/%s/%s/%s".formatted(
            repository(),
            String.join("/", group().split("\\.")),
            name(),
            version(),
            fileName()
        ));
    }

    public Artifact artifact() {
        return new Artifact(group, name, version);
    }

}
