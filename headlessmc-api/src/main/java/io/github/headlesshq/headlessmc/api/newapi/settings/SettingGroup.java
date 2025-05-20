package io.github.headlesshq.headlessmc.api.newapi.settings;

import io.github.headlesshq.headlessmc.api.HasId;
import io.github.headlesshq.headlessmc.api.HasName;
import io.github.headlesshq.headlessmc.api.command.HasDescription;

public interface SettingGroup extends HasName, HasId, HasDescription, Iterable<Setting<?>> {
    <V> Setting<V> get(Setting<V> setting, Class<V> type);

    void add(SettingBuilder<?> setting);

    void remove(Setting<?> setting);

}
