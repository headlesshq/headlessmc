package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.traits.HasName;

public interface SettingGroup extends HasName, HasDescription {
    <V> SettingBuilder<V> setting(Class<V> type);

    SettingGroup group(String name, String description);

    Iterable<SettingGroup> groups();

    Iterable<SettingKey<?>> keys();

}
