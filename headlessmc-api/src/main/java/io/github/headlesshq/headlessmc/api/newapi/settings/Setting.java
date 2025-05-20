package io.github.headlesshq.headlessmc.api.newapi.settings;

import io.github.headlesshq.headlessmc.api.HasId;
import io.github.headlesshq.headlessmc.api.HasName;
import io.github.headlesshq.headlessmc.api.command.HasDescription;

public interface Setting<V> extends HasName, HasId, HasDescription {
    V get();

    void set(SetContext context, V value);

    V getDefault();

    Class<V> getType();

    Parser<V> getParser();

    static <T> SettingBuilder<T> builder(Class<T> type) {
        return new SettingBuilder<>(type);
    }

    enum SetContext {
        CONFIG,
        USER
    }

}
