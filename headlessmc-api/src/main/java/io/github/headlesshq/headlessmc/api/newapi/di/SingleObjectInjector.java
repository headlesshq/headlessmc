package io.github.headlesshq.headlessmc.api.newapi.di;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SingleObjectInjector implements Injector {
    private final Object object;

    @Override
    public <T> T getInstance(Class<T> clazz) throws InjectorException {
        if (clazz.isInstance(object)) {
            return clazz.cast(object);
        }

        throw new InjectorException("Object is not an instance of " + clazz.getName());
    }

    @Override
    public boolean isGuaranteedToInject(Class<?> clazz) {
        return clazz.isInstance(object);
    }

}
