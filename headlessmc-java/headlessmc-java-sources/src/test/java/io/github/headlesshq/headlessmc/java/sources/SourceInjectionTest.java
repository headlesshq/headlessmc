package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that the sources of this module wire up in a real container. */
@QuarkusTest
public class SourceInjectionTest {
    @Inject
    JavaService javaService;

    @Inject
    JdksSource jdksSource;

    @Test
    public void testJdksSource() {
        // ~/.jdks does not have to exist, but scanning it must not blow up
        assertNotNull(jdksSource.getJavas());
        assertEquals("user.home.jdks", jdksSource.getName());
    }

    @Test
    public void testJavaService() {
        List<String> names = javaService.getSources().stream().map(JavaSource::getName).toList();

        assertTrue(names.contains("current"), () -> "was: " + names);
        assertTrue(names.contains("config"), () -> "was: " + names);
        assertTrue(names.contains("headlessmc"), () -> "was: " + names);
        assertTrue(names.contains(jdksSource.getName()), () -> "was: " + names);
    }

}
