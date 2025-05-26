package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.traits.HasAliases;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface NullableSettingKey<V> extends HasName, HasDescription, HasAliases {
    Supplier<@Nullable V> getDefaultValue(Config config);

    Class<V> getType();

    Parser<V> getParser(Config config);

}
