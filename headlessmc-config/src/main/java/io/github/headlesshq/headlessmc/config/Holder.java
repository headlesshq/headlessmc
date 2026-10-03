package io.github.headlesshq.headlessmc.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.function.Supplier;

/**
 * We need to work around limitations in SmallRye and microprofile config.
 * These libraries generally follow a one-time configuration approach,
 * where the configuration is loaded at application startup.
 * While there are some possibilities for changing configurations at
 * runtime they are very limited.
 * E.g. one of the few options to have a dynamic property is using a
 * {@link Provider} or {@link Supplier} like this:
 * <pre>
 * {@code
 * @Inject
 * @ConfigProperty(name = "some.property")
 * Provider<String> someProperty;
 * }
 * </pre>
 * But any use of {@link ConfigProperties} or {@link ConfigMapping} will
 * always inject the same Bean, and use of {@link Provider}/{@link Supplier}
 * for fields in {@link ConfigProperties}/{@link ConfigMapping} Beans
 * will not work, as there is no {@link Converter} for these types.
 * That means these Beans will not be updated if a config property changes,
 * no matter how you inject them.
 * <p>
 * Our solution is the {@link ConfigService}.
 * This class manages its own {@link SmallRyeConfig}.
 * Whenever a property changes, we create a new {@link SmallRyeConfig}
 * and now return new, updated Beans via the new configs
 * {@link SmallRyeConfig#getConfigMapping(Class)}.
 * <p>
 * Implementations of this class are handed out by a {@link Produces}
 * method in the {@link ConfigService} implementation,
 * they delegate to the {@link SmallRyeConfig} managed by the
 * {@link ConfigService} and if that config changes,
 * {@link #get()} returns a new and updated Bean.
 * <p>
 * Usage example:
 * <pre>
 * {@code
 * @ConfigMapping(prefix = "com.example")
 * interface ExampleConfig extends DynamicConfig {
 *      String value();
 * }
 *
 * @ApplicationScoped
 * class ExampleBean {
 *      @Inject
 *      Holder<ExampleConfig> config;
 *
 *      @Inject
 *      @ConfigMapping(prefix = "other.prefix")
 *      Holder<ExampleConfig> config2;
 * }
 * }
 * </pre>
 *
 * @param <C> the type of {@link ConfigMapping} annotated Bean held by this Holder.
 * @apiNote Holders are restricted to {@link ConfigMapping} annotated
 * Beans only!
 */
public interface Holder<C extends DynamicConfig> {
    /**
     * Returns a {@link ConfigMapping} annotated Bean.
     * The Bean returned may change between calls.
     *
     * @return the held config value.
     */
    C get();

}
