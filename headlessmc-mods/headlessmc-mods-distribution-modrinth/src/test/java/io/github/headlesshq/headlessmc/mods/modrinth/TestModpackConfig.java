package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.config.Holder;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

import java.util.Map;

final class TestModpackConfig {
    private TestModpackConfig() {
        throw new AssertionError();
    }

    /**
     * @return a {@link ModpackConfig} which does not retry, so failing downloads fail fast.
     */
    static Holder<ModpackConfig> holder() {
        return holder(Map.of(ModpackConfig.PREFIX + ".parallel.retry-max-attempts", "1"));
    }

    static Holder<ModpackConfig> holder(Map<String, String> properties) {
        ModpackConfig config = new SmallRyeConfigBuilder()
            .withMapping(ModpackConfig.class)
            .withSources(new PropertiesConfigSource(properties, "test"))
            .build()
            .getConfigMapping(ModpackConfig.class);
        return () -> config;
    }

}
