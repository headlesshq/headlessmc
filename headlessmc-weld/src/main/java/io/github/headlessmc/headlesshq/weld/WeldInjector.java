package io.github.headlessmc.headlesshq.weld;

import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.di.InjectorException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jboss.weld.environment.se.WeldContainer;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class WeldInjector implements Injector {
    private final WeldContainer container;

    @Override
    public <T> T getInstance(Class<T> clazz) throws InjectorException {
        try {
            return container.select(clazz).get();
        } catch (Exception e) {
            throw new InjectorException(e);
        }
    }

}
