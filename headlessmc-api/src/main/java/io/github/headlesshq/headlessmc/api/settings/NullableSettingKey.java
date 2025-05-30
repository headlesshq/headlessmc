package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.traits.HasAliases;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface NullableSettingKey<V> extends HasName, HasDescription, HasAliases {
    // TODO: it would be nice if we could support CDI nicely for this
    Supplier<@Nullable V> getDefaultValue(Config config);

    Class<V> getType();

    // TODO: it would be nice if we could support CDI nicely for this
    Parser<V> getParser(Config config);

}
