package io.github.headlesshq.headlessmc.api.newapi.command.picoli;

import io.github.headlesshq.headlessmc.api.newapi.di.Injector;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

@RequiredArgsConstructor
class CommandFactory implements CommandLine.IFactory {
    private final Injector injector;

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        return injector.getInstance(cls);
    }

}
