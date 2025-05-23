package io.github.headlesshq.headlessmc.api.settings;

import org.jetbrains.annotations.Nullable;

public interface NullableSettingKey<V> extends SettingKey<@Nullable V> {
    @Nullable V getDefaultValue(Config config);

}
