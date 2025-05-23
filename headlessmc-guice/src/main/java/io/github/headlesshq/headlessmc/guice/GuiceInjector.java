package io.github.headlesshq.headlessmc.guice;

import com.google.inject.ConfigurationException;
import com.google.inject.ProvisionException;
import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.di.InjectorException;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GuiceInjector implements Injector {
    private final com.google.inject.Injector injector;

    @Override
    public <T> T getInstance(Class<T> clazz) throws InjectorException {
        try {
            return injector.getInstance(clazz);
        } catch (ProvisionException | ConfigurationException e) {
            throw new InjectorException(e);
        }
    }

}
