package io.github.headlesshq.headlessmc.api.settings;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

final class Parsers {
    private static final Map<Class<?>, Parser<?>> builtInParsers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <V> @Nullable Parser<V> findParser(Class<V> type) {
        return (Parser<V>) builtInParsers.get(type);
    }

    private static <V> Parser<V> register(Class<V> type, Parser<V> parser) {
        builtInParsers.put(type, parser);
        return parser;
    }

    /**
     *  Arrays
     */

    static {
        register(Boolean.class, new AbstractParser<Boolean>(Boolean.class, "true", "false") {
            @Override
            public Boolean parse(String value) {
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(value);
                }

                throw new ParseException(String.format("Invalid boolean value: '%s'", value));
            }
        });

        register(String.class, new AbstractParser<String>(String.class) {
            @Override
            public String parse(String value) {
                return value;
            }
        });

        Parser<Long> longParser = register(Long.class, new AbstractParser<Long>(Long.class) {
            @Override
            public Long parse(String value) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new ParseException(e.getMessage());
                }
            }
        });

        register(Byte.class, longParser.map(Byte.class, Long::byteValue));
        register(Short.class, longParser.map(Short.class, Long::shortValue));
        register(Integer.class, longParser.map(Integer.class, Long::intValue));

        Parser<Double> doubleParser = register(Double.class, new AbstractParser<Double>(Double.class) {
            @Override
            public Double parse(String value) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new ParseException(e.getMessage());
                }
            }
        });

        register(Float.class, doubleParser.map(Float.class, Double::floatValue));
    }

}
