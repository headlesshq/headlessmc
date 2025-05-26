package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.traits.HasAliases;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface SettingKey<V> extends HasName, HasDescription, HasAliases, NullableSettingKey<V> {
    @Override
    Supplier<@NotNull V> getDefaultValue(Config config);

}
