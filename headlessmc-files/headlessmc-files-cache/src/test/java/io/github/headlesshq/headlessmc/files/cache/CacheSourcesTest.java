package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CacheSourcesTest {
    public record Example(String name) implements ReflectionRegistered {}

    private final DefaultJacksonJsonService jsonService = new DefaultJacksonJsonService();
    private final TypeLiteral<Example> type = new TypeLiteral<>() {};

    // ------------------------------------------------------- JsonCacheFile

    private JsonCacheFile<Example> file(CacheExceptionHandler handler, Path path) {
        return JsonCacheFile.of(handler, jsonService, type, 1L, path);
    }

    @Test
    void storesAndReadsACacheFile(@TempDir Path root) {
        Path path = root.resolve("nested").resolve("cache.json");
        JsonCacheFile<Example> cacheFile = file(CacheExceptionHandler.logging(), path);

        cacheFile.store(new CacheObject<>(1L, new Example("a")));

        assertTrue(Files.exists(path));
        assertEquals(Optional.of(new Example("a")), cacheFile.get());
    }

    @Test
    void aMissingCacheFileIsEmpty(@TempDir Path root) {
        assertEquals(Optional.empty(), file(CacheExceptionHandler.logging(), root.resolve("missing.json")).get());
    }

    @Test
    void aMissingCacheFileIsReportedByTheThrowingHandler(@TempDir Path root) {
        assertThrows(CacheException.class, () -> file(CacheExceptionHandler.throwing(), root.resolve("x.json")).get());
    }

    @Test
    void aCacheFileOfAnotherVersionIsEmpty(@TempDir Path root) {
        Path path = root.resolve("cache.json");
        file(CacheExceptionHandler.logging(), path).store(new CacheObject<>(1L, new Example("a")));

        JsonCacheFile<Example> other = JsonCacheFile.of(CacheExceptionHandler.logging(), jsonService, type, 2L, path);
        assertEquals(Optional.empty(), other.get());
    }

    @Test
    void aBrokenCacheFileIsEmpty(@TempDir Path root) throws IOException {
        Path path = Files.writeString(root.resolve("cache.json"), "not json");

        assertEquals(Optional.empty(), file(CacheExceptionHandler.logging(), path).get());
        assertThrows(CacheException.class, () -> file(CacheExceptionHandler.throwing(), path).get());
    }

    @Test
    void storingToAnUnwritableLocationIsReported(@TempDir Path root) throws IOException {
        // a file where a directory is expected makes createDirectories fail
        Path blocker = Files.writeString(root.resolve("blocker"), "");
        Path path = blocker.resolve("nested").resolve("cache.json");

        assertThrows(
            CacheException.class,
            () -> file(CacheExceptionHandler.throwing(), path).store(new CacheObject<>(1L, new Example("a")))
        );
    }

    @Test
    void cacheFilesHaveAReadableToString(@TempDir Path root) {
        assertTrue(file(CacheExceptionHandler.logging(), root.resolve("cache.json")).toString().contains("cache.json"));
    }

    // --------------------------------------------------------- JsonResource

    @Test
    void readsAJsonResourceFromTheClassLoader() {
        JsonResource<Example> resource = new JsonResource<>(
            CacheExceptionHandler.logging(), jsonService, type,
            JsonResource.getDefaultClassLoader(), "cache/example.json"
        );

        assertEquals(Optional.of(new Example("resource")), resource.get());
        assertTrue(resource.toString().contains("example.json"));
    }

    @Test
    void missingResourcesAreEmpty() {
        JsonResource<Example> resource = new JsonResource<>(
            CacheExceptionHandler.logging(), jsonService, type,
            JsonResource.getDefaultClassLoader(), "cache/missing.json"
        );

        assertEquals(Optional.empty(), resource.get());
    }

    @Test
    void unparseableResourcesAreEmpty() {
        AbstractJsonSource<Example> source = new AbstractJsonSource<>(
            CacheExceptionHandler.logging(), jsonService, type
        ) {
            @Override
            protected InputStream getInputStream() {
                return new ByteArrayInputStream("not json".getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String toString() {
                return "broken-source";
            }
        };

        assertEquals(Optional.empty(), source.get());
    }

    @Test
    void aFailingInputStreamIsReported() {
        AbstractJsonSource<Example> source = new AbstractJsonSource<>(
            CacheExceptionHandler.throwing(), jsonService, type
        ) {
            @Override
            protected InputStream getInputStream() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public String toString() {
                return "failing-source";
            }
        };

        assertThrows(CacheException.class, source::get);
    }

    // --------------------------------------------------------- HttpResource

    @Test
    void downloadsAJsonResource() {
        URI url = URI.create("https://example.com/cache.json");
        MockDownloadService downloads = new MockDownloadService();
        downloads.register(url, "{\"name\":\"remote\"}");

        HttpResource<Example> resource = new HttpResource<>(
            CacheExceptionHandler.logging(), downloads,
            builder -> builder.map(download -> jsonService.parse(download.getInputStream(), Example.class)),
            url
        );

        assertEquals(Optional.of(new Example("remote")), resource.get());
        assertTrue(resource.toString().contains(url.toString()));
    }

    @Test
    void failingDownloadsAreEmpty() {
        URI url = URI.create("https://example.com/missing.json");
        MockDownloadService downloads = new MockDownloadService();

        HttpResource<Example> resource = new HttpResource<>(
            CacheExceptionHandler.logging(), downloads,
            builder -> builder.map(download -> jsonService.parse(download.getInputStream(), Example.class)),
            url
        );

        assertEquals(Optional.empty(), resource.get());
    }

    @Test
    void failingDownloadsCanBeReported() {
        URI url = URI.create("https://example.com/missing.json");
        MockDownloadService downloads = new MockDownloadService();

        HttpResource<Example> resource = new HttpResource<>(
            CacheExceptionHandler.throwing(), downloads,
            builder -> builder.map(download -> jsonService.parse(download.getInputStream(), Example.class)),
            url
        );

        assertThrows(CacheException.class, resource::get);
    }

    // -------------------------------------------------- CacheExceptionHandler

    @Test
    void theLoggingHandlerNeverThrows() {
        CacheExceptionHandler handler = CacheExceptionHandler.logging();
        CacheFunction function = new CacheFunction() {
            @Override
            public String toString() {
                return "test";
            }
        };

        assertDoesNotThrow(() -> handler.error(function, "message"));
        assertDoesNotThrow(() -> handler.error(function, "message", new IOException()));
        assertDoesNotThrow(() -> handler.info(function, "message"));
    }

    @Test
    void theThrowingHandlerAlwaysThrows() {
        CacheExceptionHandler handler = CacheExceptionHandler.throwing();
        CacheFunction function = new CacheFunction() {
            @Override
            public String toString() {
                return "test";
            }
        };

        assertThrows(CacheException.class, () -> handler.error(function, "message"));
        assertThrows(CacheException.class, () -> handler.error(function, "message", new IOException()));
        assertThrows(CacheException.class, () -> handler.info(function, "message"));
    }

    @Test
    void tieredCachesIterateFromTheLowestLevel() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> Optional.of("first"))
            .withSource(() -> Optional.of("second"))
            .build();

        assertEquals(List.of("first", "second"), cache.stream().toList());
        assertEquals(Optional.of("second"), cache.get());
    }

}
