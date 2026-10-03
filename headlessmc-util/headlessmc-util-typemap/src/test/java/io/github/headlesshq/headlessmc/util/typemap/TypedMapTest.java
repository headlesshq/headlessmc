package io.github.headlesshq.headlessmc.util.typemap;

import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypedMapTest {
    @Test
    void shouldPutAndGetValue() {
        TypedMap map = new TypedMapImpl();
        Key<String> key = new Key<>(String.class, "name");

        assertNull(map.put(key, "Steve"));
        assertEquals("Steve", map.get(key));
    }

    @Test
    void shouldReplaceExistingValue() {
        TypedMap map = new TypedMapImpl();
        Key<Integer> key = new Key<>(Integer.class, "number");

        map.put(key, 1);

        Integer previous = map.put(key, 2);

        assertEquals(1, previous);
        assertEquals(2, map.get(key));
    }

    @Test
    void shouldRemoveValue() {
        TypedMap map = new TypedMapImpl();
        Key<String> key = new Key<>(String.class, "name");

        map.put(key, "Alex");

        assertEquals("Alex", map.remove(key));
        assertNull(map.get(key));
    }

    @Test
    void shouldReturnNullWhenKeyMissing() {
        TypedMap map = new TypedMapImpl();

        assertNull(map.get(new Key<>(String.class, "missing")));
        assertNull(map.remove(new Key<>(String.class, "missing")));
    }

    @Test
    void shouldSupportDifferentTypes() {
        TypedMap map = new TypedMapImpl();

        Key<String> stringKey = new Key<>(String.class, "string");
        Key<Integer> intKey = new Key<>(Integer.class, "int");
        Key<List<String>> listKey = new Key<>(new TypeLiteral<>() {}, "list");

        map.put(stringKey, "hello");
        map.put(intKey, 42);
        map.put(listKey, List.of("a", "b"));

        assertEquals("hello", map.get(stringKey));
        assertEquals(42, map.get(intKey));
        assertEquals(List.of("a", "b"), map.get(listKey));
    }

    @Test
    void shouldIterateOverEntries() {
        TypedMap map = new TypedMapImpl();

        Key<String> first = new Key<>(String.class, "first");
        Key<Integer> second = new Key<>(Integer.class, "second");

        map.put(first, "one");
        map.put(second, 2);

        boolean foundFirst = false;
        boolean foundSecond = false;

        for (TypedMap.Entry<?> entry : map) {
            if (entry.key().equals(first)) {
                assertEquals("one", entry.value());
                foundFirst = true;
            } else if (entry.key().equals(second)) {
                assertEquals(2, entry.value());
                foundSecond = true;
            } else {
                fail("Unexpected key: " + entry.key());
            }
        }

        assertTrue(foundFirst);
        assertTrue(foundSecond);
    }

    @Test
    void shouldReturnEmptyIteratorWhenEmpty() {
        TypedMap map = new TypedMapImpl();

        Iterator<TypedMap.Entry<?>> iterator = map.iterator();

        assertFalse(iterator.hasNext());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        TypedMap first = new TypedMapImpl();
        TypedMap second = new TypedMapImpl();

        Key<String> key = new Key<>(String.class, "key");

        first.put(key, "value");
        second.put(key, "value");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldImplementToString() {
        TypedMap map = new TypedMapImpl();
        Key<String> key = new Key<>(String.class, "name");

        map.put(key, "Steve");

        String string = map.toString();

        assertTrue(string.contains("Steve"));
        assertTrue(string.contains("name"));
    }

    @Test
    void shouldReturnDefaultValueWhenKeyIsMissing() {
        TypedMap map = new TypedMapImpl();
        Key<String> key = new Key<>(String.class, "name");

        assertEquals("default", map.get(key, "default"));
    }

    @Test
    void shouldReturnStoredValueInsteadOfDefault() {
        TypedMap map = new TypedMapImpl();
        Key<String> key = new Key<>(String.class, "name");

        map.put(key, "Steve");

        assertEquals("Steve", map.get(key, "default"));
    }

}
