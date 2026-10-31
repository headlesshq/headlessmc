package io.github.headlesshq.headlessmc.plugins.gradle;

import java.util.Collection;
import java.util.Map;

/**
 * Writes maps, lists, strings, numbers and booleans as pretty-printed JSON, so the plugin needs no dependencies.
 */
final class Json {
    private static final String INDENT = "  ";

    private Json() {
        throw new AssertionError();
    }

    static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        write(builder, value, "");
        return builder.append('\n').toString();
    }

    private static void write(StringBuilder builder, Object value, String indent) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                builder.append("{}");
                return;
            }

            builder.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                builder.append(indent).append(INDENT);
                string(builder, entry.getKey().toString());
                builder.append(": ");
                write(builder, entry.getValue(), indent + INDENT);
                builder.append(++i < map.size() ? ",\n" : "\n");
            }

            builder.append(indent).append('}');
        } else if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                builder.append("[]");
                return;
            }

            builder.append("[\n");
            int i = 0;
            for (Object element : collection) {
                builder.append(indent).append(INDENT);
                write(builder, element, indent + INDENT);
                builder.append(++i < collection.size() ? ",\n" : "\n");
            }

            builder.append(indent).append(']');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else {
            string(builder, value.toString());
        }
    }

    private static void string(StringBuilder builder, String string) {
        builder.append('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }

        builder.append('"');
    }

}
