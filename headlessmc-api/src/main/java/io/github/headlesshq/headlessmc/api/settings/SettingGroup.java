package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.traits.HasName;

import java.util.List;
import java.util.Set;

public interface SettingGroup extends HasName, HasDescription {
    <V> SettingBuilder<V> setting(Class<V> type);

    SettingGroup group(String name, String description);

    Iterable<SettingGroup> groups();

    Iterable<SettingKey<?>> keys();

    // ---- Collection Settings ---- //

    <V> CollectionSettingBuilder<V, List<V>> list(Class<V> type);

    <V> CollectionSettingBuilder<V, Set<V>> set(Class<V> type);


}
