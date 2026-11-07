package io.github.headlesshq.headlessmc.console.prompt;

import io.github.headlesshq.headlessmc.console.Password;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The answers to the {@link Prompt}s of a {@link Form}, by {@link Prompt#getId()}.
 * Closing the answers clears all {@link Password}s.
 */
public final class Answers implements AutoCloseable {
    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * Sets the answer to a prompt.
     *
     * @param id    the id of the prompt.
     * @param value a {@link String} for {@link Prompt.Type#TEXT} and {@link Prompt.Type#CHOICE},
     *              a {@link Password} for {@link Prompt.Type#SECRET}
     *              and a {@link Boolean} for {@link Prompt.Type#CONFIRM}.
     * @return these answers.
     */
    public Answers put(String id, Object value) {
        Object previous = values.put(id, value);
        if (previous instanceof Password && previous != value) {
            ((Password) previous).close();
        }

        return this;
    }

    public boolean contains(String id) {
        return values.containsKey(id);
    }

    /**
     * @return the answer to a {@link Prompt.Type#TEXT} or {@link Prompt.Type#CHOICE} prompt.
     */
    public String getString(String id) {
        return get(id, String.class);
    }

    /**
     * @return the answer to a {@link Prompt.Type#SECRET} prompt, do not close it, close these answers instead.
     */
    public Password getPassword(String id) {
        return get(id, Password.class);
    }

    /**
     * @return the answer to a {@link Prompt.Type#CONFIRM} prompt.
     */
    public boolean getBoolean(String id) {
        return get(id, Boolean.class);
    }

    private <T> T get(String id, Class<T> type) {
        Object value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException("No answer for prompt " + id);
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Answer for prompt " + id + " is a "
                + value.getClass().getSimpleName() + ", not a " + type.getSimpleName());
        }

        return type.cast(value);
    }

    @Override
    public void close() {
        for (Object value : values.values()) {
            if (value instanceof Password) {
                ((Password) value).close();
            }
        }
    }

}
