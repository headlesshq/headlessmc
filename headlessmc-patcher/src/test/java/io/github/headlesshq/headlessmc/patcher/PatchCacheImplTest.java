package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.AppFiles;
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
            TestPatchers.appFiles(root),
            TestPatchers.mcFiles(root)
        );
    }

    private PatchContext context(Path root, PatchCacheImpl cache, Classpath initial, Classpath current) {
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

    private Classpath classpath(Path... files) {
        return new Classpath(new LinkedHashSet<>(List.of(files)), new LinkedHashSet<>());
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
        Classpath initial = classpath(library);

        cache.saveCache(context(root, cache, initial, initial));
        Optional<Classpath> loaded = cache.getCache(context(root, cache, initial, initial));

        assertTrue(loaded.isPresent());
        assertEquals(initial, loaded.get());
    }

    @Test
    public void cacheIsInvalidatedWhenClasspathGrows(@TempDir Path root) throws IOException {
        McFiles mcFiles = TestPatchers.mcFiles(root);
        Path library = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("lib.jar"), "a.txt", "aaa");
        Path extra = TestPatchers.writeJar(mcFiles.getLibraryDir().resolve("extra.jar"), "b.txt", "bbb");
        PatchCacheImpl cache = cache(root);
        Classpath initial = classpath(library);

        PatchContext saved = context(root, cache, initial, initial);
        cache.saveCache(saved);

        // reuse the cache dir of the saved context but present a different initial classpath
        Classpath grown = classpath(library, extra);
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
        Classpath initial = classpath(library);
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
        Classpath initial = classpath(library);

        assertTrue(cache.getCache(context(root, cache, initial, initial)).isEmpty());
    }

}
