package io.github.headlesshq.headlessmc.api.di;

@FunctionalInterface
public interface Injector {
    <T> T getInstance(Class<T> clazz) throws InjectorException;

}
