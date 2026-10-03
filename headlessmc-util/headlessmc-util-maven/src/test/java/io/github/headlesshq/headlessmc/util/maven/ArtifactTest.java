package io.github.headlesshq.headlessmc.util.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactTest {
    @Test
    void testArtifactWithoutClassifier() {
        Artifact artifact = new Artifact("org.example", "test-artifact", "1.0.0");
        assertEquals("org.example", artifact.group());
        assertEquals("test-artifact", artifact.name());
        assertEquals("1.0.0", artifact.version());
        assertNull(artifact.classifier());
        assertEquals("org/example/test-artifact/1.0.0", artifact.path());
        assertEquals("test-artifact-1.0.0.jar", artifact.jar());
        assertEquals("test-artifact-1.0.0.pom", artifact.getFileName("pom"));
    }

    @Test
    void testArtifactWithClassifier() {
        Artifact artifact = new Artifact("org.example", "test-artifact", "1.0.0", "sources");
        assertEquals("org.example", artifact.group());
        assertEquals("test-artifact", artifact.name());
        assertEquals("1.0.0", artifact.version());
        assertEquals("sources", artifact.classifier());
        assertEquals("org/example/test-artifact/1.0.0", artifact.path());
        assertEquals("test-artifact-1.0.0-sources.jar", artifact.jar());
        assertEquals("test-artifact-1.0.0-sources.pom", artifact.getFileName("pom"));
    }

    @Test
    void testOfWithoutClassifier() {
        Artifact artifact = Artifact.of("org.example:test-artifact:1.0.0");
        assertEquals("org.example", artifact.group());
        assertEquals("test-artifact", artifact.name());
        assertEquals("1.0.0", artifact.version());
        assertNull(artifact.classifier());
    }

    @Test
    void testOfWithClassifier() {
        Artifact artifact = Artifact.of("org.example:test-artifact:1.0.0:sources");
        assertEquals("org.example", artifact.group());
        assertEquals("test-artifact", artifact.name());
        assertEquals("1.0.0", artifact.version());
        assertEquals("sources", artifact.classifier());
    }

    @Test
    void testOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Artifact.of("org.example:test-artifact"));
        assertThrows(IllegalArgumentException.class, () -> Artifact.of("org.example:test-artifact:1.0.0:sources:extra"));
    }

}
