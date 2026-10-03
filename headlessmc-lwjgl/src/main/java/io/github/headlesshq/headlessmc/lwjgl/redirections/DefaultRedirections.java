package io.github.headlesshq.headlessmc.lwjgl.redirections;

import lombok.experimental.UtilityClass;
import io.github.headlesshq.headlessmc.lwjgl.api.Redirection;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class DefaultRedirections {
    public static final Redirection NULL =
        (obj, desc, type, args) -> null;
    public static final Redirection BOOLEAN =
        (obj, desc, type, args) -> false;
    public static final Redirection BYTE =
        (obj, desc, type, args) -> (byte) 0;
    public static final Redirection SHORT =
        (obj, desc, type, args) -> (short) 0;
    public static final Redirection INT =
        (obj, desc, type, args) -> (int) 0;
    public static final Redirection LONG =
        (obj, desc, type, args) -> (long) 0;
    public static final Redirection FLOAT =
        (obj, desc, type, args) -> (float) 0.0f;
    public static final Redirection DOUBLE =
        (obj, desc, type, args) -> (double) 0.0d;
    public static final Redirection CHAR =
        (obj, desc, type, args) -> (char) 'a';
    public static final Redirection STRING =
        (obj, desc, type, args) -> "";
    public static final Redirection EQUALS =
        (obj, desc, type, args) -> args[0] == args[1];
    public static final Redirection HASHCODE =
        (obj, desc, type, args) -> System.identityHashCode(args[0]);
    private static final Map<Class<?>, Redirection> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put(void.class, NULL);
        DEFAULTS.put(boolean.class, BOOLEAN);
        DEFAULTS.put(byte.class, BYTE);
        DEFAULTS.put(short.class, SHORT);
        DEFAULTS.put(int.class, INT);
        DEFAULTS.put(long.class, LONG);
        DEFAULTS.put(float.class, FLOAT);
        DEFAULTS.put(double.class, DOUBLE);
        DEFAULTS.put(char.class, CHAR);
        DEFAULTS.put(String.class, STRING);
        DEFAULTS.put(CharSequence.class, STRING);
    }

    public static Redirection fallback(Class<?> type, Redirection object) {
        return DEFAULTS.getOrDefault(type, object);
    }

}
