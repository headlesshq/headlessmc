package io.github.headlesshq.headlessmc.api.newapi.settings;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

@Data
public class SettingGroupImpl implements SettingGroup {
    private final String name;
    private final String description;
    private final int id;

    @Override
    public <V> Setting<V> get(Setting<V> setting, Class<V> type) {
        return null;
    }

    @Override
    public void add(SettingBuilder<?> setting) {

    }

    @Override
    public void remove(Setting<?> setting) {

    }

    @Override
    public @NotNull Iterator<Setting<?>> iterator() {
        return null;
    }

}
