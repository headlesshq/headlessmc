package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.command.Suggestion;

import java.util.List;
import java.util.function.Function;

public interface Parser<V> {
    String toParseableString(V value);

    V parse(String value) throws ParseException;

    Class<V> getType();

    List<Suggestion> getSuggestions();

    List<Suggestion> getSuggestions(String value, int cursor);

    default <T> Parser<T> map(Class<T> type, Function<V, T> mapper) {
        return new AbstractParser<T>(type) {
            @Override
            public T parse(String value) throws ParseException {
                return mapper.apply(Parser.this.parse(value));
            }
        };
    }

}
