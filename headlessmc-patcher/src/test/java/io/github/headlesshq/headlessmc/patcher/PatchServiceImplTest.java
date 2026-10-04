package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PatchServiceImplTest {
    /** Records the contexts it was called with. */
    static final class RecordingPatcher implements Patcher {
        private final List<PatchContext> contexts = new ArrayList<>();
        private final String name;

        RecordingPatcher(String name) {
            this.name = name;
        }

        @Override
        public void patch(PatchContext context) {
            contexts.add(context);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long version() {
            return 1L;
        }
    }

    /** Contributes the patchers the service discovers, in the order they are declared. */
    static class Patchers {
        @Produces
        @Singleton
        @Named("b")
        Patcher b() {
            return new RecordingPatcher("b");
        }

        @Produces
        @Singleton
        @Named("a")
        Patcher a() {
            return new RecordingPatcher("a");
        }
    }

    @Inject
    HashService hashService;

    @Inject
    JsonService jsonService;

    @Inject
    FileService fileService;

    @Inject
    @Any
    Instance<HelperService> helperServices;

    @Inject
    @Any
    Instance<Patcher> patchers;

    @Inject
    @Named("a")
    Patcher a;

    @Inject
    @Named("b")
    Patcher b;

    @BeforeEach
    void resetPatchers() {
        ((RecordingPatcher) a).contexts.clear();
        ((RecordingPatcher) b).contexts.clear();
    }

    private PatchServiceImpl service(Path root) {
        PatchCacheImpl cache = new PatchCacheImpl(
            fileService, hashService, jsonService, TestPatchers.appFiles(root), TestPatchers.mcFiles(root)
        );

        return new PatchServiceImpl(helperServices, patchers, fileService, cache, TestPatchers.mcFiles(root));
    }

    private Classpath classpath(Path root) throws IOException {
        return new Classpath(new LinkedHashSet<>(
            List.of(TestPatchers.writeJar(root.resolve("mc.jar"), "a.class", "content"))
        ), new LinkedHashSet<>());
    }

    @Test
    public void withoutPatchersTheClasspathIsUnchanged(@TempDir Path root) throws IOException {
        Classpath classpath = classpath(root);

        assertSame(classpath, service(root).patch(classpath, 21, List.of()));
    }

    @Test
    public void everyPatcherIsRunAndTheResultIsCached(@TempDir Path root) throws IOException {
        PatchServiceImpl service = service(root);
        Classpath classpath = classpath(root);

        Classpath result = service.patch(classpath, 21, List.of(a, b));

        assertEquals(classpath.files(), result.files());
        assertEquals(1, ((RecordingPatcher) a).contexts.size());
        assertEquals(1, ((RecordingPatcher) b).contexts.size());
        assertEquals(21, ((RecordingPatcher) a).contexts.getFirst().getJavaVersion());

        // the second run is served from the cache
        service.patch(classpath, 21, List.of(a, b));
        assertEquals(1, ((RecordingPatcher) a).contexts.size());
    }

    @Test
    public void patchersAreListedInTheirNaturalOrder(@TempDir Path root) {
        assertEquals(List.of(a, b), service(root).getPatchers());
    }

    @Test
    public void patchersCanBeLookedUpByNameIgnoringCase(@TempDir Path root) {
        PatchServiceImpl service = service(root);

        assertEquals(Optional.of(a), service.getPatcher("A"));
        assertEquals(Optional.empty(), service.getPatcher("nope"));
    }

}
