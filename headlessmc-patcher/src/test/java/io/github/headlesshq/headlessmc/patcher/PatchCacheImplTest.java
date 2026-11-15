package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PatchCacheImplTest {
    @Inject
    HashService hashService;

    @Inject
    JsonService jsonService;

    @Inject
    FileService fileService;

    private PatchCacheImpl cache(Path root) {
        return new PatchCacheImpl(
            fileService,
            hashService,
            jsonService,
            TestPatchers.appFiles(root)
        );
    }

    private PatchContext context(Path root, PatchCacheImpl cache, PatchResult initial, PatchResult current) {
        PatchCache.Key key = cache.getCacheKey(initial, List.of(TestPatchers.patcher("test", 1L)), 21);
        return new PatchContextImpl(
            Stream::empty,
            initial,
            fileService,
            key,
            List.of(TestPatchers.patcher("test", 1L)),
            TestPatchers.mcFiles(root),
            21,
            cache.getCacheDir(key),
            current
        );
    }

    private PatchResult classpath(Path... files) {
        return new PatchResult(new LinkedHashSet<>(List.of(files)), new LinkedHashSet<>());
    }

    @Test
    public void cacheKeyDescribesClasspathAndPatchers(@TempDir Path root) throws IOException {
        Path a = TestPatchers.writeJar(root.resolve("a.jar"), "a.txt", "aaa");
        Path b = TestPatchers.writeJar(root.resolve("b.jar"), "b.txt", "bbb");
        PatchCacheImpl cache = cache(root);

        PatchCache.Key key = cache.getCacheKey(
            classpath(a, b),
            List.of(TestPatchers.patcher("one", 1L), TestPatchers.patcher("two", 2L)),
            21
        );

        HashService.HashResult expected = hashService.hashFiles(List.of(a, b), HashService.SHA256);
        assertEquals(expected.hash(), key.sha256());
        assertEquals(expected.size(), key.size());
        assertEquals(Map.of("one", 1L, "two", 2L), key.patchers());
        assertEquals(21, key.javaVersion());
        assertEquals(cache.getVersion(), key.version());
    }

    @Test
    public void cacheDirIsStableForEqualKeys(@TempDir Path root) throws IOException {
        Path a = TestPatchers.writeJar(root.resolve("a.jar"), "a.txt", "aaa");
        PatchCacheImpl cache = cache(root);
        List<Patcher> patchers = List.of(TestPatchers.patcher("one", 1L));

        Path first = cache.getCacheDir(cache.getCacheKey(classpath(a), patchers, 21));
        Path second = cache.getCacheDir(cache.getCacheKey(classpath(a), patchers, 21));
        Path other = cache.getCacheDir(cache.getCacheKey(classpath(a), patchers, 17));

        assertEquals(first, second);
        assertNotEquals(first, other);
        assertTrue(first.startsWith(TestPatchers.appFiles(root).getCacheDir()));
    }

    @Test
    public void savedCacheCanBeLoaded(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        PatchCacheImpl cache = cache(root);
        PatchResult initial = classpath(library);

        cache.saveCache(context(root, cache, initial, initial));
        Optional<PatchResult> loaded = cache.getCache(context(root, cache, initial, initial));

        assertTrue(loaded.isPresent());
        assertEquals(initial, loaded.get());
    }

    @Test
    public void savedCacheRestoresJavaAgentsAndSystemProperties(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        PatchCacheImpl cache = cache(root);
        PatchResult initial = classpath(library);
        PatchCache.Key key = cache.getCacheKey(initial, List.of(TestPatchers.patcher("test", 1L)), 21);
        Path agent = TestPatchers.writeJar(
            cache.getCacheDir(key).resolve("test").resolve("agent.jar"), "Agent.class", "agent");
        PatchResult patched = initial
            .withAgent(agent)
            .withSystemProperty("joml.nounsafe", "true")
            .withSystemProperty("flag", null);

        cache.saveCache(context(root, cache, initial, patched));
        Optional<PatchResult> loaded = cache.getCache(context(root, cache, initial, initial));

        assertTrue(loaded.isPresent());
        assertEquals(patched, loaded.get());
        assertEquals(List.of("joml.nounsafe", "flag"), List.copyOf(loaded.get().systemProperties().keySet()));
    }

    @Test
    public void cacheIsInvalidWhenJavaAgentIsMissing(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        PatchCacheImpl cache = cache(root);
        PatchResult initial = classpath(library);
        PatchCache.Key key = cache.getCacheKey(initial, List.of(TestPatchers.patcher("test", 1L)), 21);
        Path missingAgent = cache.getCacheDir(key).resolve("test").resolve("agent.jar");

        cache.saveCache(context(root, cache, initial, initial.withAgent(missingAgent)));

        assertTrue(cache.getCache(context(root, cache, initial, initial)).isEmpty());
    }

    @Test
    public void cacheIsInvalidatedWhenClasspathGrows(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        Path extra = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("extra.jar"), "b.txt", "bbb");
        PatchCacheImpl cache = cache(root);
        PatchResult initial = classpath(library);

        PatchContext saved = context(root, cache, initial, initial);
        cache.saveCache(saved);

        // reuse the cache dir of the saved context but present a different initial classpath
        PatchResult grown = classpath(library, extra);
        PatchContext invalid = new PatchContextImpl(
            Stream::empty,
            grown,
            fileService,
            saved.getCacheKey(),
            List.of(TestPatchers.patcher("test", 1L)),
            mcFiles,
            21,
            cache.getCacheDir(saved.getCacheKey()),
            grown
        );

        assertTrue(cache.getCache(invalid).isEmpty());
        assertTrue(Files.notExists(cache.getCacheDir(saved.getCacheKey()).resolve("cache.json")),
            "invalid cache should be deleted");
    }

    @Test
    public void cacheIsInvalidWhenPatchedFileIsMissing(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        PatchCacheImpl cache = cache(root);
        PatchResult initial = classpath(library);
        PatchCache.Key key = cache.getCacheKey(initial, List.of(TestPatchers.patcher("test", 1L)), 21);
        Path missingPatched = cache.getCacheDir(key).resolve("patched").resolve("lib.jar");

        cache.saveCache(context(root, cache, initial, classpath(library, missingPatched)));

        assertTrue(cache.getCache(context(root, cache, initial, initial)).isEmpty());
    }

    @Test
    public void getCacheIsEmptyWithoutSavedCache(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        PatchCacheImpl cache = cache(root);
        PatchResult initial = classpath(library);

        assertTrue(cache.getCache(context(root, cache, initial, initial)).isEmpty());
    }

    private PatchContext context(McFiles mcFiles, PatchCacheImpl cache, PatchResult initial, PatchResult current) {
        PatchCache.Key key = cache.getCacheKey(initial, List.of(TestPatchers.patcher("test", 1L)), 21);
        return new PatchContextImpl(
            Stream::empty, initial, fileService, key, List.of(TestPatchers.patcher("test", 1L)),
            mcFiles, 21, cache.getCacheDir(key), current
        );
    }

    @Test
    public void aCacheIsUsedForTheSameLibrariesInAnotherMinecraftDirectory(@TempDir Path root) throws IOException {
        // the same libraries installed in two .minecraft directories, e.g. on two disks
        McFiles mcFiles = TestPatchers.mcFiles(root);
        McFiles other = McFiles.of(root.resolve("other-mc"));
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("org").resolve("lib.jar"), "a.txt", "aaa");
        Path otherLibrary = other.getLibraryDir().resolve("org").resolve("lib.jar");
        Files.createDirectories(otherLibrary.getParent());
        Files.copy(library, otherLibrary);

        PatchCacheImpl cache = cache(root);
        PatchResult initial = classpath(library);
        PatchCache.Key key = cache.getCacheKey(initial, List.of(TestPatchers.patcher("test", 1L)), 21);
        Path patched = TestPatchers.writeJar(
            cache.getCacheDir(key).resolve("test").resolve("patched.jar"), "b.txt", "bbb");
        // the patchers added a jar and left the library on the classpath
        cache.saveCache(context(mcFiles, cache, initial, classpath(library, patched)));

        Optional<PatchResult> loaded = cache.getCache(context(other, cache, classpath(otherLibrary), classpath(otherLibrary)));

        // patching is not repeated, and the library is the one of the directory that is launched
        assertTrue(loaded.isPresent());
        assertEquals(List.of(otherLibrary, patched), List.copyOf(loaded.get().files()));
        // and the cache still works for the first directory
        assertEquals(
            List.of(library, patched),
            List.copyOf(cache.getCache(context(mcFiles, cache, initial, initial)).orElseThrow().files())
        );
    }

    @Test
    public void aCacheForOtherLibrariesIsNotUsed(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        McFiles other = McFiles.of(root.resolve("other-mc"));
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        Path otherLibrary = TestPatchers.writeJar(other.getLibraryDir().resolve("lib.jar"), "a.txt", "different");
        PatchCacheImpl cache = cache(root);
        cache.saveCache(context(mcFiles, cache, classpath(library), classpath(library)));

        assertEquals(
            Optional.empty(),
            cache.getCache(context(other, cache, classpath(otherLibrary), classpath(otherLibrary)))
        );
    }

}
