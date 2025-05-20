package io.github.headlesshq.headlessmc.api.newapi.di;

public interface Injector {
    <T> T getInstance(Class<T> clazz) throws InjectorException;

    boolean isGuaranteedToInject(Class<?> clazz);

    default Object getHandle() {
        return this;
    }

}
