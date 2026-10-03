package io.github.headlesshq.headlessmc.java.args;

import org.jspecify.annotations.Nullable;

/**
 * Represents an argument pair, like {@code --arg <value>}.
 * Can also represent a SystemProperty: {@code -Darg=value},
 * or positional parameters {@code arg}, or arguments without
 * a value {@code --list}.
 *
 * @param arg the argument, can also be a positional parameter.
 * @param value optionally, the value passed for the argument.
 */
public record ArgPair(String arg, @Nullable String value) {
    public String[] asArray() {
        return value == null
              ? new String[]{arg}
              : new String[]{arg, value};
    }

}
