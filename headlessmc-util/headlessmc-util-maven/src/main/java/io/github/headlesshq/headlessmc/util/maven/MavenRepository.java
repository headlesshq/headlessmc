package io.github.headlesshq.headlessmc.util.maven;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;

/**
 * Represents a Maven repository that you can find {@link Artifact}s in.
 */
@Data
@Getter
@RequiredArgsConstructor
public class MavenRepository {
    public static final MavenRepository CENTRAL = MavenRepository.of("https://repo1.maven.org/maven2");

    private final URI url;

    public URI getJar(Artifact artifact) {
        return URI.create("%s/%s/%s".formatted(url, artifact.path(), artifact.jar()));
    }

    public static MavenRepository of(String url) {
        return new MavenRepository(URI.create(url));
    }

}
