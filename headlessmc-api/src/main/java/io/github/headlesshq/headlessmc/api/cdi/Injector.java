package io.github.headlesshq.headlessmc.api.cdi;

@FunctionalInterface
public interface Injector {
    <T> T getInstance(Class<T> clazz) throws InjectorException;

}
