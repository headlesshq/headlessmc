package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.command.Splitter;
import io.github.headlesshq.headlessmc.api.command.Suggestion;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
final class CollectionParser<V, C extends Collection<V>> implements Parser<C> {
    private final Supplier<C> collectionFactory;
    private final Splitter splitter;
    private final Parser<V> parser;
    private final char delimiter;

    @Override
    public String toParseableString(C value) {
        // TODO: do we need to escape?!
        StringBuilder result = new StringBuilder();
        Iterator<V> iterator = value.iterator();
        while (iterator.hasNext()) {
            result.append(parser.toParseableString(iterator.next()));
            if (iterator.hasNext()) {
                result.append(delimiter);
            }
        }

        return result.toString();
    }

    @Override
    public C parse(String value) throws ParseException {
        String[] tokens = splitter.split(value);
        C collection = collectionFactory.get();
        for (String token : tokens) {
            collection.add(parser.parse(token));
        }

        return collection;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<C> getType() {
        return (Class<C>) collectionFactory.get().getClass();
    }

    @Override
    public List<Suggestion> getSuggestions() {
        return parser.getSuggestions();
    }

    @Override
    public List<Suggestion> getSuggestions(String value, int cursor) {
        String sub = value.substring(cursor);
        String[] split = splitter.split(sub);
        if (split.length > 0) {
            String last = split[split.length - 1];
            return parser.getSuggestions(last, last.length()); // < TODO
        }

        return new ArrayList<>();
    }

}
