package io.github.headlesshq.headlessmc.api.di;

public interface Injector {
    <T> T getInstance(Class<T> clazz) throws InjectorException;

    Object getHandle();

}
