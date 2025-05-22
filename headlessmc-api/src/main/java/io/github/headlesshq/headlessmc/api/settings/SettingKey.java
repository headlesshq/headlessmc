package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.traits.HasAliases;
import io.github.headlesshq.headlessmc.api.traits.HasDescription;
import io.github.headlesshq.headlessmc.api.traits.HasName;

public interface SettingKey<V> extends HasName, HasDescription, HasAliases {
    Class<V> getType();

    V getDefaultValue(Config config);

    Parser<V> getParser(Config config);

}
