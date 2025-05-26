package io.github.headlesshq.headlessmc.api.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.inject.Inject;

@ApplicationScoped
@SuppressWarnings("CdiInjectionPointsInspection") // yes you can inject BeanContainer!
public class BeanInjector implements Injector {
    private final Instance<Object> instance;

    @Inject
    public BeanInjector(BeanContainer beanContainer) {
        this.instance = beanContainer.createInstance();
    }

    @Override
    public <T> T getInstance(Class<T> clazz) throws InjectorException {
        return instance.select(clazz).get();
    }

}
