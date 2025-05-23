package io.github.headlesshq.headlessmc.api;

import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.di.InjectorException;
import picocli.CommandLine;

public class TestInjector implements Injector {
    @Override
    public <T> T getInstance(Class<T> clazz) throws InjectorException {
        try {
            return CommandLine.defaultFactory().create(clazz);
        } catch (Exception e) {
            throw new InjectorException(e);
        }
    }

}
