package io.github.headlesshq.headlessmc.api.command.picocli;

import io.github.headlesshq.headlessmc.api.di.Injector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
class CommandFactory implements CommandLine.IFactory {
    private final Injector injector;

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        return injector.getInstance(cls);
    }

}
