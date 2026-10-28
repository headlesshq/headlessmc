package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest({
    DefaultJacksonJsonService.class,
    RelativePathService.class,
    RelativePathInjectionTest.FirstSource.class,
    RelativePathInjectionTest.SecondSource.class
})
public class RelativePathInjectionTest {
    static final Path FIRST = Path.of("first").toAbsolutePath();

    public record Holder(Path file) implements ReflectionRegistered {}

    @ApplicationScoped
    public static class FirstSource implements RelativePathSource {
        @Override
        public String getIdentifier() {
            return "first";
        }

        @Override
        public Path getPath() {
            return FIRST;
        }
    }

    @ApplicationScoped
    public static class SecondSource implements RelativePathSource {
        @Override
        public String getIdentifier() {
            return "second";
        }

        @Override
        public Path getPath() {
            return FIRST.resolve("second");
        }
    }

    @Inject
    JsonService jsonService;

    @Test
    void injectedSourcesAreUsed() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        jsonService.write(out, new Holder(FIRST.resolve("second").resolve("file")), false);
        String json = out.toString(StandardCharsets.UTF_8);

        assertTrue(json.contains("\"source\":\"second\""), json);
        assertTrue(json.contains("\"path\":\"file\""), json);
    }

}
