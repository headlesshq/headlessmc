package io.github.headlesshq.headlessmc.api.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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

    @Override
    public <V> CollectionSettingBuilder<V, List<V>> list(Class<V> type) {
        return new CollectionSettingBuilderImpl<>(this, type, ArrayList::new);
    }

    @Override
    public <V> CollectionSettingBuilder<V, Set<V>> set(Class<V> type) {
        return new CollectionSettingBuilderImpl<>(this, type, LinkedHashSet::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <V> SettingKey<V> add(SettingKey<V> key) {
        String lower = key.getName().toLowerCase(Locale.ENGLISH);
        SettingReference<?> ref = settings.computeIfAbsent(lower, v -> new SettingReference.NonNull<>(key));
        if (!ref.getType().isAssignableFrom(key.getType())) {
            throw new IllegalArgumentException("Trying to register key " + key + " of type "
                    + key.getType() + " but is already registered with type " + ref.getType());
        } else if (!(ref instanceof SettingKey)) {
            throw new IllegalStateException("Trying to register non-null key " + key
                    + " but a nullable key is already registered: " + ref.reference);
        }

        ref.setReference((SettingKey) key);
        return (SettingKey) ref;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <V> NullableSettingKey<V> addNullable(NullableSettingKey<V> key) {
        String lower = key.getName().toLowerCase(Locale.ENGLISH);
        SettingReference<?> ref = settings.computeIfAbsent(lower, v -> new SettingReference<>(key));
        if (!ref.getType().isAssignableFrom(key.getType())) {
            throw new IllegalArgumentException("Trying to register key " + key + " of type "
                    + key.getType() + " but is already registered with type " + ref.getType());
        } else if (ref instanceof SettingKey) {
            // we cannot allow this because someone could try to use the old key to call set(key, null)...
            // if it was only getting it would be fine
            throw new IllegalStateException("Trying to register nullable key " + key
                    + " but a non-nullable key is already registered: " + ref.reference);
        }

        ref.setReference((NullableSettingKey) key);
        return (NullableSettingKey) ref;
    }

    @AllArgsConstructor
    private static class SettingReference<V> implements NullableSettingKey<V> {
        protected volatile NullableSettingKey<V> reference;

        protected void setReference(NullableSettingKey<V> ref) {
            this.reference = ref;
        }

        @Override
        public Class<V> getType() {
            return reference.getType();
        }

        @Override
        public Supplier<V> getDefaultValue(Config config) {
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

        public static final class NonNull<V> extends SettingReference<V> implements SettingKey<V> {
            public NonNull(SettingKey<V> reference) {
                super(reference);
            }

            @Override
            protected void setReference(NullableSettingKey<V> ref) {
                if (!(ref instanceof SettingKey<?>)) {
                    throw new IllegalArgumentException("Trying to set non-nullable key " + ref
                            + " on nullable setting key " + reference);
                }

                super.setReference(ref);
            }
        }
    }

}
