package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TieredCacheTest {
    private TestCacheSource level1;
    private TestCacheSource level2;
    private TestCacheSource level3;
    private TestCacheStore store1;
    private TestCacheStore store2;
    private Cache<String> cache;

    @BeforeEach
    void setup() throws HeadlessMcException {
        level1 = new TestCacheSource();
        level2 = new TestCacheSource();
        level3 = new TestCacheSource();

        store1 = new TestCacheStore();
        store2 = new TestCacheStore();

        cache = createCache();

        assertInstanceOf(TieredCache.class, cache);
    }

    protected Cache<String> createCache() {
        return CacheBuilder.<String>create()
            .withStore(store1)
            .withSource(level1)
            .withStore(store2)
            .withSource(level2)
            .withSource(level3)
            .build();
    }

    @Test
    void shouldUseHighestLevelSourceFirst() throws HeadlessMcException {
        level1.setValue("level1");
        level2.setValue("level2");
        level3.setValue("level3");

        assertEquals(Optional.of("level3"), cache.get());

        assertEquals("level3", store1.value);
        assertEquals("level3", store2.value);

        assertEquals(0, level1.getCalls());
        assertEquals(0, level2.getCalls());
        assertEquals(1, level3.getCalls());
    }

    @Test
    void shouldFallbackToLowerLevels() throws HeadlessMcException {
        level1.setValue("level1");
        level2.setValue("level2");
        // level3 returns Optional.empty in this test

        assertEquals(Optional.of("level2"), cache.get());

        assertEquals("level2", store1.value);
        assertEquals("level2", store2.value);

        assertEquals(0, level1.getCalls());
        assertEquals(1, level2.getCalls());
        assertEquals(1, level3.getCalls());
    }

    @Test
    void shouldFallbackToLowestLevel() throws HeadlessMcException {
        level1.setValue("level1");

        assertEquals(Optional.of("level1"), cache.get());

        assertEquals("level1", store1.value);
        // store2 is on a higher level than 1, and will not store from that source
        assertNull(store2.value);

        assertEquals(1, level1.getCalls());
        assertEquals(1, level2.getCalls());
        assertEquals(1, level3.getCalls());
    }

    @Test
    void shouldReturnEmptyWhenNothingAvailable() throws HeadlessMcException {
        assertEquals(Optional.empty(), cache.get());

        assertNull(store1.value);
        assertNull(store2.value);

        assertEquals(1, level1.getCalls());
        assertEquals(1, level2.getCalls());
        assertEquals(1, level3.getCalls());
    }

    @Test
    void shouldOnlyResolveSourcesOnce() throws HeadlessMcException {
        level3.setValue("cached");

        assertEquals(Optional.of("cached"), cache.get());
        assertEquals(Optional.of("cached"), cache.get());
        assertEquals(Optional.of("cached"), cache.get());

        assertEquals("cached", store1.value);
        assertEquals("cached", store2.value);

        assertEquals(1, store1.calls);
        assertEquals(1, store2.calls);
        assertEquals(1, level3.getCalls());
    }

    @Test
    void shouldReturnPreviousValueOnSet() throws HeadlessMcException {
        level3.setValue("before");

        assertEquals(Optional.of("before"), cache.get());

        assertEquals(
            Optional.of("before"),
            cache.set("after")
        );

        assertEquals("after", store1.value);
        assertEquals("after", store2.value);
        assertEquals(Optional.of("after"), cache.get());
    }

    @Test
    void shouldReturnPreviousValueOnClear() throws HeadlessMcException {
        level3.setValue("value");
        assertEquals(Optional.of("value"), cache.get());

        assertEquals(
            Optional.of("value"),
            cache.clear()
        );

        level3.setValue("level3");
        assertEquals(Optional.of("level3"), cache.get());
    }

    @Test
    void shouldUpdateAllLevelsOnSet() throws HeadlessMcException {
        cache.set("new-value");

        assertEquals(Optional.of("new-value"), cache.get());
    }

    @Test
    void shouldInvalidateCachedStateAfterClear() throws HeadlessMcException {
        level3.setValue("first");

        assertEquals(Optional.of("first"), cache.get());

        cache.clear();

        level3.setValue("second");

        assertEquals(Optional.of("second"), cache.get());
    }

    @Test
    void shouldExposeConfiguredVersion() {
        Cache<String> cache = CacheBuilder.<String>create()
            .withVersion(42)
            .withSource(level1)
            .withSource(level2)
            .build();

        assertInstanceOf(TieredCache.class, cache);
        assertEquals(42, cache.getVersion());
    }

    @Getter
    @Setter
    @ToString
    private static final class TestCacheSource implements CacheSource<String> {
        private @Nullable String value;
        private int calls;

        @NonNull
        @Override
        public Optional<String> get() {
            calls++;
            return Optional.ofNullable(value);
        }
    }

    @Getter
    @Setter
    @ToString
    private static final class TestCacheStore implements CacheStore<String> {
        private @Nullable String value;
        private int calls;

        @Override
        public void store(@NonNull CacheObject<String> object) {
            calls++;
            this.value = object.object();
        }
    }

}
