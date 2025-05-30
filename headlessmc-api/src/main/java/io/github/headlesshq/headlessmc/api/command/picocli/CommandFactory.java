package io.github.headlesshq.headlessmc.api.command.picocli;

import io.github.headlesshq.headlessmc.api.cdi.Injector;
import io.github.headlesshq.headlessmc.api.cdi.InjectorException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CommandFactory implements CommandLine.IFactory {
    private final Injector injector;

    @Override
    public <K> K create(Class<K> cls) throws InjectorException {
        return injector.getInstance(cls);
    }

}
