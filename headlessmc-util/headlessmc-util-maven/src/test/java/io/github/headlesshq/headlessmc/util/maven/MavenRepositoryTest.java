package io.github.headlesshq.headlessmc.util.maven;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenRepositoryTest {
    @Test
    void testGetJar() {
        MavenRepository repository = MavenRepository.of("https://repo.maven.apache.org/maven2");
        Artifact artifact = new Artifact("org.example", "test-artifact", "1.0.0");
        URI expected = URI.create("https://repo.maven.apache.org/maven2/org/example/test-artifact/1.0.0/test-artifact-1.0.0.jar");
        assertEquals(expected, repository.getJar(artifact));
    }

    @Test
    void testGetJarWithClassifier() {
        MavenRepository repository = MavenRepository.of("https://repo.maven.apache.org/maven2");
        Artifact artifact = new Artifact("org.example", "test-artifact", "1.0.0", "sources");
        URI expected = URI.create("https://repo.maven.apache.org/maven2/org/example/test-artifact/1.0.0/test-artifact-1.0.0-sources.jar");
        assertEquals(expected, repository.getJar(artifact));
    }

}
