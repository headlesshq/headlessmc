package io.github.headlesshq.headlessmc.patcher.probe.strategy;

import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.asm.SuperClassResolver;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Launches the real probe.jar through the {@link io.github.headlesshq.headlessmc.java.JavaService},
 * so it needs an installed Java (or a download) for the current feature version.
 */
@QuarkusTest
@Disabled("Integration test, launches a real Java process")
class SuperClassProbeStrategyIT {
    @Inject
    SuperClassProbeStrategy strategy;

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void probeResolvesCommonSuperClasses() {
        PatchContext context = Mockito.mock(PatchContext.class);
        Mockito.when(context.getJavaVersion()).thenReturn(Runtime.version().feature());

        try (SuperClassResolver resolver = strategy.apply(context)) {
            assertEquals("java/util/AbstractList",
                resolver.getCommonSuperClass("java/util/ArrayList", "java/util/LinkedList"));
            assertEquals("java/util/AbstractCollection",
                resolver.getCommonSuperClass("java/util/ArrayList", "java/util/ArrayDeque"));
            assertEquals("java/util/List",
                resolver.getCommonSuperClass("java/util/List", "java/util/ArrayList"));
            assertEquals("java/lang/Object",
                resolver.getCommonSuperClass("java/lang/Runnable", "java/lang/String"));

            assertThrows(PatchException.class,
                () -> resolver.getCommonSuperClass("does/not/Exist", "java/lang/String"));
            // the process is still alive after an error
            assertEquals("java/lang/Number",
                resolver.getCommonSuperClass("java/lang/Integer", "java/lang/Long"));
        }
    }

}
