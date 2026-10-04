package io.github.headlesshq.headlessmc.config;

import io.github.headlesshq.headlessmc.exceptions.ExceptionUtil;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ConfigService}.
 * Contains the {@link Produces} method for {@link Holder}
 * with {@link #createHolder(InjectionPoint)}.
 */
@Slf4j
@ApplicationScoped
public class ConfigServiceImpl implements ConfigService {
    /**
     * Ordinal of the properties set at runtime.
     * Higher than system properties (400), so that changes at runtime always take effect.
     */
    public static final int RUNTIME_ORDINAL = 800;
    /**
     * Ordinal of the config file.
     * Higher than application.properties (250), but lower than
     * environment variables (300) and system properties (400).
     */
    public static final int CONFIG_FILE_ORDINAL = 270;

    private final Instance<DynamicConfig> dynamicConfigs;
    private final org.eclipse.microprofile.config.Config baseConfig;

    private volatile State state;

    @Inject
    public ConfigServiceImpl(
        @Any Instance<DynamicConfig> dynamicConfigs,
        org.eclipse.microprofile.config.Config config
    ) {
        this(dynamicConfigs, new ConcurrentHashMap<>(), config);
    }

    public ConfigServiceImpl(
        Instance<DynamicConfig> dynamicConfigs,
        Map<String, String> properties,
        org.eclipse.microprofile.config.Config config
    ) {
        this(dynamicConfigs, config, properties, Map.of(), null);
    }

    private ConfigServiceImpl(
        Instance<DynamicConfig> dynamicConfigs,
        org.eclipse.microprofile.config.Config baseConfig,
        Map<String, String> properties,
        Map<String, String> fileProperties,
        @Nullable Path configFile
    ) {
        this.dynamicConfigs = dynamicConfigs;
        this.baseConfig = baseConfig;
        this.state = buildState(Map.copyOf(properties), Map.copyOf(fileProperties), configFile);
    }

    /**
     * Loads the config file from the {@link ConfigFileProvider}, if one exists, on startup.
     * This cannot happen in the constructor, as the location of the config file
     * might depend on config itself, e.g. via the {@link Holder}s for the OS config.
     *
     * @param event the startup event.
     * @param configFileProvider the provider for the config file location.
     */
    void onStartup(@Observes StartupEvent event, Instance<ConfigFileProvider> configFileProvider) {
        if (configFileProvider.isResolvable()) {
            loadConfigFile(configFileProvider.get().getConfigFile());
        } else {
            log.debug("No ConfigFileProvider, not using a config file.");
        }
    }

    /**
     * Reads the properties from the given config file
     * and uses the file to persist properties to from now on.
     *
     * @param configFile the config file to use, does not need to exist.
     * @throws ConfigException if the file cannot be read or contains invalid values.
     */
    public synchronized void loadConfigFile(Path configFile) throws ConfigException {
        Map<String, String> fileProperties;
        try {
            fileProperties = PropertiesFile.load(configFile);
        } catch (IOException | IllegalArgumentException e) { // IllegalArgumentException for malformed unicode escapes
            throw new ConfigException("Failed to read config file " + configFile, e);
        }

        try {
            this.state = buildState(state.properties, Map.copyOf(fileProperties), configFile);
        } catch (ConfigException e) {
            throw new ConfigException("Invalid config file " + configFile + ": " + e.getMessage(), e);
        }

        log.debug("Loaded {} properties from config file {}", fileProperties.size(), configFile);
    }

    /**
     * @return the config file used to persist properties, if there is one.
     */
    public Optional<Path> getConfigFile() {
        return Optional.ofNullable(state.configFile);
    }

    @Override
    public Config getConfig() {
        return state.config;
    }

    @Override
    public synchronized boolean set(String name, String value, boolean persist) {
        Objects.requireNonNull(value, "value was null");
        State state = this.state;
        Path configFile = state.configFile;
        if (persist && configFile == null) {
            throw new ConfigException("Cannot persist " + name + ", this config is not backed by a config file.");
        }

        boolean changesProperties = !value.equals(state.properties.get(name));
        boolean changesFile = persist && !value.equals(state.fileProperties.get(name));
        if (!changesProperties && !changesFile) {
            return false;
        }

        Map<String, String> properties = with(state.properties, name, value);
        Map<String, String> fileProperties = persist ? with(state.fileProperties, name, value) : state.fileProperties;
        // building the new state validates the value, if it is invalid we neither use nor persist it
        State newState = buildState(properties, fileProperties, configFile);
        if (persist) {
            try {
                PropertiesFile.set(configFile, name, value);
            } catch (ConfigurationException e) {
                throw new ConfigException("Failed to write " + name + " to config file " + configFile, e);
            }
        }

        this.state = newState;
        return true;
    }

    @Override
    public synchronized boolean remove(String name) {
        State state = this.state;
        if (!state.properties.containsKey(name)) {
            return false;
        }

        Map<String, String> properties = new HashMap<>(state.properties);
        properties.remove(name);
        this.state = buildState(Map.copyOf(properties), state.fileProperties, state.configFile);
        return true;
    }

    @Override
    public ConfigService fork() {
        State state = this.state;
        return new ConfigServiceImpl(dynamicConfigs, baseConfig, state.properties, state.fileProperties, null);
    }

    @Override
    public <C extends DynamicConfig> Holder<C> getHolder(Class<C> type) {
        return createHolder(type, null);
    }

    @Override
    public <C extends DynamicConfig> Holder<C> getHolder(Class<C> type, String prefix) {
        return createHolder(type, Objects.requireNonNull(prefix, "Prefix was null"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V bind(Object key, Supplier<V> mapping) {
        return (V) state.cache.computeIfAbsent(mapping, _ -> mapping.get());
    }

    @Override
    public SortedSet<String> getPropertyNames() {
        SortedSet<String> propertyNames = new TreeSet<>();
        for (String propertyName : state.config.getPropertyNames()) {
            propertyNames.add(propertyName);
        }

        return propertyNames;
    }

    @Produces
    @Dependent
    @SuppressWarnings("unchecked")
    public <C extends DynamicConfig> Holder<C> createHolder(InjectionPoint point) {
        String prefix = null;
        if (point.getAnnotated().isAnnotationPresent(ConfigMapping.class)) {
            ConfigMapping mapping = point.getAnnotated().getAnnotation(ConfigMapping.class);
            if (!"".equals(mapping.prefix())) {
                prefix = mapping.prefix();
            }
        }

        Class<?> type;
        if (point.getType() instanceof ParameterizedType parameterizedType) {
            Type[] args = parameterizedType.getActualTypeArguments();
            if (args.length != 1) {
                throw new IllegalArgumentException(
                    "Injection point " + point + " had != 1 type parameter: " + Arrays.toString(args)
                );
            }

            if (args[0] instanceof Class<?> clazz) {
                type = clazz;
            } else {
                throw new IllegalArgumentException(
                    "Failed to determine holder type " + args[0] + " for injection point " + point
                );
            }
        } else {
            throw new IllegalArgumentException("Holder type needs to be parameterized: " + point);
        }

        return createHolder((Class<C>) type, prefix);
    }

    private <C extends DynamicConfig> Holder<C> createHolder(Class<C> type, @Nullable String prefix) {
        try {
            if (prefix == null) {
                return () -> (C) state.config.getConfigMapping(type);
            } else {
                return () -> (C) state.config.getConfigMapping(type, prefix);
            }
        } catch (/*okay-to-catch-marker*/Exception e) {
            throw new ConfigException(ExceptionUtil.handleInterruptions(e));
        }
    }

    private State buildState(
        Map<String, String> properties,
        Map<String, String> fileProperties,
        @Nullable Path configFile
    ) {
        try {
            Config config = buildConfig(List.of(
                new MapConfigSource("HeadlessMc", RUNTIME_ORDINAL, properties),
                new MapConfigSource(
                    configFile == null ? ConfigFileProvider.CONFIG_FILE_NAME : configFile.toString(),
                    CONFIG_FILE_ORDINAL,
                    fileProperties
                )
            ));

            return new State(config, properties, fileProperties, configFile, new ConcurrentHashMap<>());
        } catch (ConfigException e) {
            throw e;
        } catch (/*okay-to-catch-marker*/Exception e) {
            throw new ConfigException(ExceptionUtil.handleInterruptions(e));
        }
    }

    private Config buildConfig(List<ConfigSource> additionalSources) {
        List<ConfigSource> sources = new ArrayList<>();
        // always build on top of the original config, never on top of a config we built ourselves,
        // otherwise our own sources would pile up and shadow each other
        baseConfig.getConfigSources().forEach(sources::add);
        sources.addAll(additionalSources);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
            // the builder defaults to the class loader of the thread building it, which is wrong
            // as soon as a Bean asks for its config from a thread we do not control,
            // e.g. one of the ForkJoinPool threads of a parallel stream
            .forClassLoader(ConfigServiceImpl.class.getClassLoader())
            .addDefaultInterceptors()
            //.addDefaultSources()
            .addDiscoveredConverters()
            .addDiscoveredCustomizers()
            .addDiscoveredInterceptors()
            .addDiscoveredSecretKeysHandlers()
            .addDiscoveredValidator();

        for (Instance.Handle<DynamicConfig> handle : dynamicConfigs.handles()) {
            builder.withMapping(handle.getBean().getBeanClass());
        }

        new QuarkusConfigBuilderCustomizer().configBuilder(builder);
        return builder.withSources(sources).build();
    }

    private static Map<String, String> with(Map<String, String> properties, String name, String value) {
        Map<String, String> result = new HashMap<>(properties);
        result.put(name, value);
        return Map.copyOf(result);
    }

    private record State(
        Config config,
        Map<String, String> properties,
        Map<String, String> fileProperties,
        @Nullable Path configFile,
        Map<Object, Object> cache
    ) { }

    private record MapConfigSource(String name, int ordinal, Map<String, String> properties) implements ConfigSource {
        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public @Nullable String getValue(String propertyName) {
            return properties.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }
    }

}
