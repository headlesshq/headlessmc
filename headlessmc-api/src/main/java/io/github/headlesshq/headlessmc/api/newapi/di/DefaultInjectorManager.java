package io.github.headlesshq.headlessmc.api.newapi.di;

import java.util.ArrayDeque;
import java.util.Deque;

public class DefaultInjectorManager implements InjectorManager {
    private final Deque<Injector> injectors = new ArrayDeque<>();

    @Override
    public <T> T getInstance(Class<T> clazz) throws InjectorException {
        for (Injector injector : injectors) {
            if (injector.isGuaranteedToInject(clazz)) {
                return injector.getInstance(clazz);
            }
        }

        InjectorException injectorException = new InjectorException("No suitable injector found for " + clazz);
        for (Injector injector : injectors) {
            try {
                return injector.getInstance(clazz);
            } catch (InjectorException e) {
                injectorException.addSuppressed(e);
            }
        }

        throw injectorException;
    }

    @Override
    public boolean isGuaranteedToInject(Class<?> clazz) {
        return injectors.stream().anyMatch(injector -> injector.isGuaranteedToInject(clazz));
    }

    @Override
    public Deque<Injector> getInjectors() {
        return injectors;
    }

}
