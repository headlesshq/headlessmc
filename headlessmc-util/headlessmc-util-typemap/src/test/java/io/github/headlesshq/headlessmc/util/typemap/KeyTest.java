package io.github.headlesshq.headlessmc.util.typemap;

import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeyTest {
    @Test
    void shouldCreateKeyFromClass() {
        Key<String> key = new Key<>(String.class, "test");

        assertEquals("test", key.getName());
        assertEquals(String.class, key.getType());
    }

    @Test
    void shouldCreateKeyFromTypeLiteral() {
        TypeLiteral<List<String>> literal = new TypeLiteral<>() {};

        Key<List<String>> key = new Key<>(literal, "list");

        assertEquals("list", key.getName());
        assertEquals(literal.getType(), key.getType());
    }

    @Test
    void shouldPreserveGenericType() {
        Type expected = new TypeLiteral<List<String>>() {}.getType();

        Key<List<String>> key = new Key<>(new TypeLiteral<>() {}, "list");

        assertEquals(expected, key.getType());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        Key<String> first = new Key<>(String.class, "name");
        Key<String> second = new Key<>(String.class, "name");
        Key<String> third = new Key<>(String.class, "other");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

}
