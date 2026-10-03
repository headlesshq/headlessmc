package io.github.headlesshq.headlessmc.files.cache;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class CacheViewTest {
    private Cache<String> cache(String initial) {
        return CacheBuilder.<String>create()
            .withSource(() -> Optional.of(initial))
            .build();
    }

    @Test
    void modifyWithConsumerWritesBack() {
        Cache<String> cache = cache("initial");
        AtomicBoolean seen = new AtomicBoolean();

        Optional<String> previous = cache.modify((Consumer<String>) value -> seen.set(true));

        assertEquals(Optional.of("initial"), previous);
        assertTrue(seen.get());
    }

    @Test
    void modifyWithOperatorReplacesValue() {
        Cache<String> cache = cache("initial");

        Optional<String> previous = cache.modify(value -> value + "-modified");

        assertEquals(Optional.of("initial"), previous);
        assertEquals(Optional.of("initial-modified"), cache.get());
    }

    @Test
    void maybeModifyOnlyWritesWhenRequested() {
        Cache<String> cache = cache("initial");

        cache.maybeModify(value -> false);
        assertEquals(Optional.of("initial"), cache.get());

        cache.set("changed");
        Optional<String> previous = cache.maybeModify(value -> true);
        assertEquals(Optional.of("changed"), previous);
        assertEquals(Optional.of("changed"), cache.get());
    }

    @Test
    void readOnlyViewMapsValue() {
        Cache<String> cache = cache("5");

        View<Integer> view = cache.view((value, previous) -> Integer.parseInt(value));

        assertEquals(Optional.of(5), view.get());
        // cached view state is reused while the parent does not change
        assertEquals(Optional.of(5), view.get());

        cache.set("7");
        assertEquals(Optional.of(7), view.get());
    }

    @Test
    void chainedViewsMapThroughAllMappings() {
        Cache<String> cache = cache("5");

        View<String> view = cache
            .view((value, previous) -> Integer.parseInt(value))
            .view((value, previous) -> "mapped-" + value);

        assertEquals(Optional.of("mapped-5"), view.get());
        assertTrue(view.toString().contains("mapped-5"));
    }

    @Test
    void viewIteratorMapsParentValues() {
        Cache<String> cache = cache("5");
        View<Integer> view = cache.view((value, previous) -> Integer.parseInt(value));

        assertEquals(List.of(5), view.stream().toList());
    }

    @Test
    void mutableViewReadsAndModifies() {
        Cache<String> cache = cache("5");
        Cache<StringBuilder> view = cache.mutableView(
            (value, previous) -> new StringBuilder(value),
            (value, previous) -> value.toString()
        );

        assertEquals("5", view.get().orElseThrow().toString());

        Optional<StringBuilder> previous = view.modify((Consumer<StringBuilder>) builder -> builder.append("1"));
        assertEquals("51", previous.orElseThrow().toString());
        assertEquals(Optional.of("51"), cache.get(), "in-place modification should reach the parent");
    }

    @Test
    void mutableViewMaybeModify() {
        Cache<String> cache = cache("5");
        Cache<StringBuilder> view = cache.mutableView(
            (value, previous) -> new StringBuilder(value),
            (value, previous) -> value.toString()
        );

        view.maybeModify(builder -> {
            builder.append("0");
            return true;
        });
        assertEquals(Optional.of("50"), cache.get());

        view.maybeModify(builder -> {
            builder.append("9");
            return false;
        });
        assertEquals(Optional.of("50"), cache.get(), "false means no write-back");
    }

    @Test
    void mutableViewSetReturnsPreviousValue() {
        Cache<String> cache = cache("5");
        Cache<StringBuilder> view = cache.mutableView(
            (value, previous) -> new StringBuilder(value),
            (value, previous) -> value.toString()
        );

        Optional<StringBuilder> previous = view.set(new StringBuilder("7"));

        assertEquals("5", previous.orElseThrow().toString());
        assertEquals("7", view.get().orElseThrow().toString());
    }

    @Test
    void mutableViewModifyWithOperator() {
        Cache<String> cache = cache("5");
        Cache<StringBuilder> view = cache.mutableView(
            (value, previous) -> new StringBuilder(value),
            (value, previous) -> value.toString()
        );

        Optional<StringBuilder> previous = view.modify((UnaryOperator<StringBuilder>) builder -> builder.append("2"));

        assertEquals("52", previous.orElseThrow().toString());
        assertEquals("52", view.get().orElseThrow().toString());
    }

    @Test
    void mutableViewVersionAndIterator() {
        Cache<String> cache = cache("5");
        Cache<StringBuilder> view = cache.mutableView(
            (value, previous) -> new StringBuilder(value),
            (value, previous) -> value.toString()
        );

        assertEquals(cache.getVersion(), view.getVersion());
        assertEquals(1, view.stream().count());
    }

}
