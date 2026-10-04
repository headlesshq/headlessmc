package io.github.headlesshq.headlessmc.forge.installer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code net.minecraftforge} classes on the test classpath are the stubs from
 * the {@code mock} source set: every real installer call throws, so these tests
 * cover strategy selection and error handling rather than an actual installation.
 */
class ForgeInstallerTest {
    @TempDir
    Path root;

    @Test
    void mainRequiresExactlyOneArgument() {
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{"a", "b"}));
    }

    @Test
    void mainCollectsTheFailuresOfAllStrategies() {
        RuntimeException e = assertThrows(
            RuntimeException.class, () -> Main.main(new String[]{root.toString()})
        );

        assertEquals("Failed to install Forge", e.getMessage());
        assertEquals(4, e.getSuppressed().length);
        assertEquals("true", System.getProperty("java.awt.headless"));
        assertEquals("true", System.getProperty("java.net.preferIPv4Stack"));
    }

    @Test
    void strategiesForTheStubbedClientInstallAreUsable() {
        assertTrue(new InstallationStrategyPost1_13().isUsable());
        assertTrue(new InstallationStrategy2_1().isUsable());
        assertTrue(new InstallationStrategy2_2().isUsable());
    }

    @Test
    void theLegacyStrategyIsNotUsableWithoutTheLegacyClass() {
        assertFalse(new InstallationStrategyPre1_13().isUsable());
    }

    @Test
    void strategiesFailWhenTheInstallerCannotBeUsed() {
        File target = root.toFile();

        assertThrows(Exception.class, () -> new InstallationStrategyPost1_13().install(target));
        assertThrows(Exception.class, () -> new InstallationStrategy2_1().install(target));
        assertThrows(Exception.class, () -> new InstallationStrategy2_2().install(target));
        assertThrows(Exception.class, () -> new InstallationStrategyPre1_13().install(target));
    }

    @Test
    void predicatesPreferGuavaWhenItIsOnTheClasspath() throws ClassNotFoundException {
        assertEquals(Predicate.class, Predicates.getJavaPredicateClass());
        // the mock source set provides com.google.common.base.Predicate
        assertEquals(com.google.common.base.Predicate.class, Predicates.getPredicateClass());
        assertTrue(((Predicate<Object>) Predicates.getJavaPredicate()).test("anything"));
    }

    @Test
    void googlePredicatesAreDetectedByName() {
        assertFalse(Predicates.isGooglePredicate(Predicate.class));
        assertTrue(Predicates.isGooglePredicate(com.google.common.base.Predicate.class));
        assertTrue(((com.google.common.base.Predicate<Object>) Predicates.getGooglePredicate()).test("anything"));
    }

}
