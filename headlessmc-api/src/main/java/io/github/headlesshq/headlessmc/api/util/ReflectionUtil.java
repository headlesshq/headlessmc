package io.github.headlesshq.headlessmc.api.util;

import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import io.github.headlesshq.headlessmc.api.classloading.Deencapsulator;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility for Reflection.
 */
@CustomLog
@UtilityClass
public class ReflectionUtil {
    private static final Deencapsulator DEENCAPSULATOR = new Deencapsulator();

    /**
     * Iterates over the super classes of the given class.
     *
     * @param clazz  the class whose super classes will be iterated over.
     * @param action will be applied to all classes.
     */
    public static void iterate(Class<?> clazz, Consumer<Class<?>> action) {
        iterate(clazz, c -> {
            action.accept(c);
            return null;
        });
    }

    /**
     * Iterates over the super classes of the given class. Will stop iterating
     * if the given Function returns a value which is not {@code null} and
     * returns that value.
     *
     * @param clazz  the clazz whose super classes will be iterated over.
     * @param action the function every class is applied to.
     * @param <T>    the type of value returned by the given function.
     * @return the first value returned by the given function or {@code null}.
     */
    public static <T> T iterate(Class<?> clazz, Function<Class<?>, T> action) {
        T result = null;
        Class<?> current = clazz;
        while (current != null) {
            result = action.apply(current);
            if (result != null) {
                break;
            }

            current = current.getSuperclass();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(String method, Object instance, T def, Class<?>[] parameterTypes, Object... args) {
        try {
            //DEENCAPSULATOR.deencapsulate(instance.getClass());
            Method execute = instance.getClass().getMethod(method, parameterTypes);
            execute.setAccessible(true);
            return (T) execute.invoke(instance, args);
        } catch (ReflectiveOperationException e) {
            log.error("Failed to call " + method + " on " + instance, e);
            return def;
        }
    }

}
