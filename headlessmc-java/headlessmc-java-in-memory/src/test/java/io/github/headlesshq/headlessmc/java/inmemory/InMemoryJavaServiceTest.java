package io.github.headlesshq.headlessmc.java.inmemory;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.sources.CurrentJavaSource;
import io.github.headlesshq.headlessmc.java.sources.JavaExecutableFinder;
import io.github.headlesshq.headlessmc.java.version.SpecificationVersionParser;
import io.github.headlesshq.headlessmc.os.OS;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryJavaServiceTest {
    private final CurrentJavaSource currentJavaSource = new CurrentJavaSource(
        new SpecificationVersionParser(),
        new JavaExecutableFinder(new OS("linux", OS.Type.LINUX, "6.0"))
    );
    private final InMemoryJavaService service = new InMemoryJavaService(currentJavaSource);

    @Test
    void onlyKnowsTheCurrentJava() {
        List<Java> javas = service.getJavaVersions();
        assertEquals(1, javas.size());
        assertTrue(javas.get(0).current());
        assertEquals(Runtime.version().feature(), javas.get(0).version());
        assertEquals(List.of(currentJavaSource), service.getSources());
    }

    @Test
    void servesEveryVersionWithTheCurrentJava() {
        Java current = service.getJavaVersions().get(0);
        assertEquals(current, service.getJava(Runtime.version().feature()).orElseThrow());
        assertEquals(current, service.getJava(8).orElseThrow());
        assertEquals(current, service.getJava(25).orElseThrow());
    }

}
