package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.util.maven.Artifact;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class PrismIndexTest {
    private static PrismIndex.Meta meta(String version, String mcVersion) {
        List<PrismIndex.Meta.Requires> requires = new ArrayList<>();
        requires.add(new PrismIndex.Meta.Requires("irrelevant", "net.other"));
        if (mcVersion != null) {
            requires.add(new PrismIndex.Meta.Requires(mcVersion, "net.minecraft"));
        }

        return new PrismIndex.Meta(requires, "sha", version);
    }

    @Test
    void mcVersionIsReadFromTheRequirements() {
        assertEquals("1.20.2", meta("48.1.0", "1.20.2").getMcVersion());
    }

    @Test
    void missingMcVersionThrows() {
        assertThrows(NoSuchElementException.class, () -> meta("48.1.0", null).getMcVersion());
    }

    @Test
    void equalVersionsCompareEqual() {
        assertEquals(0, meta("48.1.0", "1.20.2").compareTo(meta("48.1.0", "1.20.4")));
    }

    @Test
    void betaVersionsSortBeforeReleases() {
        assertTrue(meta("48.1.0-beta", "1.20.2").compareTo(meta("48.1.0", "1.20.2")) > 0);
        assertTrue(meta("48.1.0", "1.20.2").compareTo(meta("48.1.0-beta", "1.20.2")) < 0);
    }

    @Test
    void numericSegmentsAreComparedNumerically() {
        assertTrue(meta("48.2.0", "1.20.2").compareTo(meta("48.10.0", "1.20.2")) < 0);
        assertTrue(meta("11.15.1.2318", "1.8.9").compareTo(meta("11.15.1.2317", "1.8.9")) > 0);
    }

    @Test
    void nonNumericSegmentsAreComparedAsText() {
        assertTrue(meta("48.1.a", "1.20.2").compareTo(meta("48.1.b", "1.20.2")) < 0);
    }

    @Test
    void longerVersionsSortFirstWhenTheirPrefixIsEqual() {
        assertTrue(meta("48.1.0.1", "1.20.2").compareTo(meta("48.1.0", "1.20.2")) < 0);
    }

    @Test
    void artifactsAreResolvedFromTheIndex() {
        PrismIndex index = new PrismIndex("net.minecraftforge", List.of(meta("48.1.0", "1.20.2")));

        Artifact artifact = new ForgeArtifactResolverImpl()
            .resolve("forge", index, index.versions().getFirst(), "installer");

        assertEquals(new Artifact("net.minecraftforge", "forge", "1.20.2-48.1.0", "installer"), artifact);
    }

}
