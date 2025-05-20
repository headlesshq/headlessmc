package io.github.headlesshq.headlessmc.api.newapi.settings;

import io.github.headlesshq.headlessmc.api.newapi.command.Suggestion;

import java.util.List;

public interface Parser<V> {
    V parse(String string) throws ParseException;

    List<Suggestion> suggest(String string, int cursor);

}
