package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.command.picocli.Root;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class RootCommandProducer {
    @Root
    @Produces
    @ApplicationScoped
    public Object rootCommand() {
        return RootCommand.class;
    }

}
