package io.github.headlesshq.headlessmc.api.settings;

import lombok.Data;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// TODO: merge with other setting group?
@Data
final class SettingGroupImpl implements SettingGroup {
    private final Map<String, SettingReference<?>> settings = new ConcurrentHashMap<>();
    private final Map<String, SettingGroup> groups = new ConcurrentHashMap<>();
    private final String name;
    private final String description;

    @Override
    public <V> SettingBuilder<V> setting(Class<V> type) {
        return new SettingBuilderImpl<>(this, type);
    }

    @Override
    public SettingGroup group(String name, String description) {
        String lower = name.toLowerCase(Locale.ENGLISH);
        SettingGroup group = groups.get(lower);
        if (group == null) {
            group = new SettingGroupImpl(name, description);
            groups.put(lower, group);
        }

        return group;
    }

    @Override
    public Iterable<SettingGroup> groups() {
        return groups.values();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Iterable<SettingKey<?>> keys() {
        return (Iterable) settings.values();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <V> SettingKey<V> add(SettingKey<V> key) {
        String lower = key.getName().toLowerCase(Locale.ENGLISH);
        SettingReference<?> ref = settings.get(lower);
        if (ref == null) {
            ref = new SettingReference<>();
        } else if (!ref.getType().isAssignableFrom(key.getType())) {
            throw new IllegalArgumentException("Trying to register key " + key + " of type "
                    + key.getType() + " but is already registered with type " + ref.getType());
        }

        ref.setReference((SettingKey) key);
        return (SettingKey) ref;
    }

    private static final class SettingReference<V> implements SettingKey<V> {
        private volatile SettingKey<V> reference;

        private void setReference(SettingKey<V> ref) {
            this.reference = ref;
        }

        @Override
        public Class<V> getType() {
            return reference.getType();
        }

        @Override
        public V getDefaultValue(Config config) {
            return reference.getDefaultValue(config);
        }

        @Override
        public Parser<V> getParser(Config config) {
            return reference.getParser(config);
        }

        @Override
        public List<String> getAliases() {
            return reference.getAliases();
        }

        @Override
        public String getDescription() {
            return reference.getDescription();
        }

        @Override
        public String getName() {
            return reference.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SettingKey)) return false;
            SettingKey<?> that = (SettingKey<?>) o;
            return Objects.equals(getType(), that.getType())
                    && Objects.equals(getName(), that.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getType(), getName());
        }
    }

}
