package io.github.headlesshq.headlessmc.files.cache;

import lombok.ToString;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CacheBuilderTest {

    @Test
    void shouldBuildSingleCacheWhenOnlyOneSourceExists() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> Optional.of("value"))
            .build();

        assertEquals(Optional.of("value"), cache.get());
    }

    @Test
    void shouldBuildTieredCacheWhenMultipleSourcesExist() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> Optional.of("first"))
            .withSource(() -> Optional.of("second"))
            .build();

        assertInstanceOf(TieredCache.class, cache);
    }

    @Test
    void shouldPreferHighestLevelCache() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();

        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> {
                firstCalls.incrementAndGet();
                return Optional.of("low");
            })
            .withSource(() -> {
                secondCalls.incrementAndGet();
                return Optional.of("high");
            })
            .build();

        assertEquals(Optional.of("high"), cache.get());

        assertEquals(0, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    @Test
    void shouldFallbackToLowerLevelCache() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();

        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> {
                firstCalls.incrementAndGet();
                return Optional.of("fallback");
            })
            .withSource(() -> {
                secondCalls.incrementAndGet();
                return Optional.empty();
            })
            .build();

        assertEquals(Optional.of("fallback"), cache.get());

        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    @Test
    void shouldCacheRetrievedValue() {
        AtomicInteger calls = new AtomicInteger();

        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> {
                calls.incrementAndGet();
                return Optional.of("cached");
            })
            .build();

        assertEquals(Optional.of("cached"), cache.get());
        assertEquals(Optional.of("cached"), cache.get());
        assertEquals(Optional.of("cached"), cache.get());

        assertEquals(1, calls.get());
    }

    @Test
    void shouldReturnPreviousValueOnSet() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(CacheSource.empty())
            .build();

        assertEquals(Optional.empty(), cache.set("first"));
        assertEquals(Optional.of("first"), cache.set("second"));
        assertEquals(Optional.of("second"), cache.get());
    }

    @Test
    void shouldReturnPreviousValueOnClear() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(CacheSource.empty())
            .build();

        cache.set("value");

        assertEquals(Optional.of("value"), cache.clear());
        assertEquals(Optional.empty(), cache.get());
    }

    @Test
    void shouldPropagateRetrievedValueToLowerLevels() {
        RecordingStore<String> store = new RecordingStore<>();

        Cache<String> cache = CacheBuilder.<String>create()
            .withStore(store)
            .withSource(() -> Optional.of("remote"))
            .build();

        assertEquals(Optional.of("remote"), cache.get());
        assertEquals("remote", store.lastStored);
    }

    @Test
    void iteratorShouldWalkCachesFromLowestToHighest() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> Optional.of("classpath"))
            .withSource(() -> Optional.of("disk"))
            .withSource(() -> Optional.of("remote"))
            .build();

        Iterator<String> iterator = cache.iterator();

        assertTrue(iterator.hasNext());
        assertEquals("classpath", iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals("disk", iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals("remote", iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    void streamShouldReturnAllAvailableValues() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> Optional.of("one"))
            .withSource(() -> Optional.of("two"))
            .withSource(() -> Optional.of("three"))
            .build();

        assertEquals(
            List.of("one", "two", "three"),
            cache.stream().toList()
        );
    }

    @Test
    void clearShouldClearAllTierLevels() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withSource(() -> Optional.of("value"))
            .withSource(Optional::empty)
            .build();

        assertEquals(Optional.of("value"), cache.get());

        cache.clear();

        assertEquals(Optional.of("value"), cache.get());
    }

    @ToString
    static final class RecordingStore<T> implements CacheStore<T> {
        T lastStored;

        @Override
        public void store(CacheObject<T> object) {
            lastStored = object.object();
        }
    }

}