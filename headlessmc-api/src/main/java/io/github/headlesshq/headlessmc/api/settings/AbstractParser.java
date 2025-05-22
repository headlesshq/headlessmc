package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.command.Suggestion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public abstract class AbstractParser<V> implements Parser<V> {
    private final Class<V> type;
    private final List<Suggestion> suggestions;

    public AbstractParser(Class<V> type, String... suggestions) {
        this(
            type,
            Arrays.stream(suggestions)
                .map(s -> new Suggestion(s, null, true))
                .collect(Collectors.toList())
        );
    }

    @Override
    public String toParseableString(V value) {
        return String.valueOf(value);
    }

    @Override
    public List<Suggestion> getSuggestions(String value, int cursor) {
        String arg = value.toLowerCase(Locale.ENGLISH).substring(0, cursor);
        List<Suggestion> suggestions = new ArrayList<>();
        for (Suggestion suggestion : getSuggestions()) {
            if (suggestion.getValue().toLowerCase(Locale.ENGLISH).startsWith(arg)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }
}
