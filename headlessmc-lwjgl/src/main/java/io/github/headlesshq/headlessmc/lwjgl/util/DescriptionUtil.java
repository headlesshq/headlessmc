package io.github.headlesshq.headlessmc.lwjgl.util;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Basically {@link Type#getDescriptor(Class)} since that class might not be
 * available at Runtime.
 */
@UtilityClass
public class DescriptionUtil {
    private static final Map<Class<?>, String> PRIMITIVES = new HashMap<>();

    static {
        PRIMITIVES.put(boolean.class, "Z");
        PRIMITIVES.put(byte.class, "B");
        PRIMITIVES.put(short.class, "S");
        PRIMITIVES.put(int.class, "I");
        PRIMITIVES.put(long.class, "J");
        PRIMITIVES.put(float.class, "F");
        PRIMITIVES.put(double.class, "D");
        PRIMITIVES.put(char.class, "C");
        PRIMITIVES.put(void.class, "V");
    }

    public static String getDesc(Method method) {
        StringBuilder desc = new StringBuilder(method.getName()).append("(");
        for (Class<?> parameter : method.getParameterTypes()) {
            putDesc(parameter, desc);
        }

        putDesc(method.getReturnType(), desc.append(")"));
        return desc.toString();
    }

    public static String getDesc(Class<?> type) {
        StringBuilder result = new StringBuilder();
        putDesc(type, result);
        return result.toString();
    }

    private static void putDesc(Class<?> type, StringBuilder builder) {
        while (type.isArray()) {
            builder.append("[");
            type = type.getComponentType();
        }

        String primitive = PRIMITIVES.get(type);
        if (primitive == null) {
            builder.append("L");
            String name = type.getName();
            for (int i = 0; i < name.length(); i++) {
                char ch = name.charAt(i);
                builder.append(ch == '.' ? '/' : ch);
            }

            builder.append(";");
        } else {
            builder.append(primitive);
        }
    }

}
