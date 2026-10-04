package io.github.headlesshq.headlessmc.config;

import io.smallrye.config.Config;
import io.smallrye.config.ConfigMapping;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.SortedSet;
import java.util.function.Supplier;

/**
 * To work around limitations of SmallRyeConfig and Microprofile Config
 * we create our own config management system.
 * Mainly this enables configuration of config properties at runtime,
 * via {@link Holder}s that hold {@link ConfigMapping}
 * annotated Beans.
 */
public interface ConfigService {
    /**
     * Returns the currently active config.
     * This value may change between calls,
     * guaranteed when {@link #set(String, String, boolean)}
     * is called with a new property value.
     *
     * @return the currently active config.
     */
    Config getConfig();

    // TODO: throw if unknown property

    /**
     * Sets the config property with the given name to a new value.
     * The new value takes effect immediately for the rest of this process,
     * taking precedence over every other {@link ConfigSource}, including SystemProperties.
     * <p>
     * If {@code persist} is {@code true}, the value is additionally written to the config file
     * (config.properties), so that it is also used the next time HeadlessMc starts.
     * Values in the config file have a lower priority than environment variables and SystemProperties.
     *
     * @param name    the name of the config property.
     * @param value   the value of the config property.
     * @param persist {@code true} to also write the value to the config file,
     *                {@code false} to only keep it in memory for the rest of this process.
     * @return {@code true} if the property has changed as a result of this operation, or {@code false},
     * if it had the same value before this call.
     * Note that {@code true} might even be returned if the value reported
     * by this config was the same before the call, but sourced
     * from another {@link ConfigSource} such as SystemProperties.
     * @throws ConfigException if the value is invalid, the property does not exist,
     * or {@code persist} is {@code true} and the value could not be written to the config file,
     * e.g. because this config service has no config file, like {@link #fork()}ed ones.
     */
    boolean set(String name, String value, boolean persist) throws ConfigException;

    /**
     * Removes a property that has been {@link #set(String, String, boolean)} at runtime.
     * The value returned by the {@link Config},
     * will now be the value supplied by {@link ConfigSource}s
     * of lower ordinals, e.g. SystemProperties, env variables, the config file etc.
     * This does not remove the property from the config file.
     *
     * @param name the name of the property to remove.
     * @return {@code true} if the property has been removed, or {@code false}
     * if the property was never in this config to begin with.
     * @throws ConfigException if something goes wrong building the config.
     */
    boolean remove(String name) throws ConfigException;

    /**
     * Creates a new, independent ConfigService which inherits the properties
     * that have been {@link #set(String, String, boolean)} for this config service,
     * and the properties of its config file.
     * The fork is not backed by the config file itself, so it cannot persist properties.
     *
     * @return a new, independent ConfigService.
     * @throws ConfigException if something goes wrong when creating a {@link Config}.
     */
    ConfigService fork();

    /**
     * Programmatic method to get a {@link Holder}.
     * This is useful for getting a Holder of
     * {@link #fork()}ed {@link ConfigService}s.
     *
     * @param type the type of the interface with
     *             {@link ConfigMapping} annotation to wrap.
     * @param <C>  the type of the interface with
     *             {@link ConfigMapping} annotation to wrap.
     * @return a Holder bound to this {@link ConfigService}.
     * @throws ConfigException if SmallRye fails to validate the config.
     */
    <C extends DynamicConfig> Holder<C> getHolder(Class<C> type);

    /**
     * Programmatic method to get a {@link Holder},
     * with a certain prefix ({@link ConfigMapping#prefix()}).
     * This is useful for getting a Holder of
     * {@link #fork()}ed {@link ConfigService}s.
     *
     * @param type   the type of the interface with
     * @param prefix the prefix of the {@link ConfigMapping}.
     *               {@link ConfigMapping} annotation to wrap.
     * @param <C>    the type of the interface with
     *               {@link ConfigMapping} annotation to wrap.
     * @return a Holder bound to this {@link ConfigService}.
     * @throws ConfigException if SmallRye fails to validate the config.
     */
    <C extends DynamicConfig> Holder<C> getHolder(Class<C> type, String prefix);

    /**
     * Binds a computation of some value to a config.
     * The result of the computation is cached until the config changes.
     *
     * @param key the key to store the cache with (lambdas have bad guarantees for hashcode and equals).
     * @param mapping the computation that produces the value to cache.
     * @return the result of the computation or a new value if the config changed.
     * @param <V> the type of the result.
     */
    <V> V bind(Object key, Supplier<V> mapping);

    /**
     * @return {@link Config#getPropertyNames()} for the current config.
     */
    SortedSet<String> getPropertyNames();

}
