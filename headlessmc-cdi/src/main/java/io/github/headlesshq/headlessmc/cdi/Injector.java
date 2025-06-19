package io.github.headlesshq.headlessmc.cdi;

/**
 * An Injector represents a dependency injection tool that can create instances of a class.
 * E.g. an {@link jakarta.enterprise.inject.Instance}.
 */
@FunctionalInterface
public interface Injector {
    <T> T getInstance(Class<T> clazz) throws InjectorException;

}
