package io.github.headlesshq.headlessmc.util.typemap;

import jakarta.enterprise.util.TypeLiteral;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * A typed key used to store and retrieve values from a {@link TypedMap}.
 * <p>
 * A key is uniquely identified by its {@code name} and {@code type}. Two keys
 * are considered equal only if both properties are equal.
 * <p>
 * Generic types can be preserved by constructing a key from a {@link TypeLiteral},
 * allowing keys such as {@code List<String>} to be distinguished from
 * {@code List<Integer>}.
 *
 * @param <V> the value type associated with this key
 */
@Data
public class Key<V> {
    /**
     * The unique name of this key.
     */
    private final String name;
    /**
     * The Java type represented by this key.
     */
    private final Type type;

    /**
     * Creates a key for a raw class type.
     *
     * @param type the value type
     * @param name the unique key name
     */
    public Key(Class<V> type, String name) {
        this(name, type);
    }

    /**
     * Creates a key for a generic type.
     *
     * @param type the generic value type
     * @param name the unique key name
     */
    public Key(TypeLiteral<V> type, String name) {
        this(name, type.getType());
    }

    private Key(String name, Type type) {
        this.name = name;
        this.type = type;
    }

}
