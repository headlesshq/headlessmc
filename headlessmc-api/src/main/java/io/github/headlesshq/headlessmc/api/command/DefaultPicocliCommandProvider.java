package io.github.headlesshq.headlessmc.api.command;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPicocliCommandProvider implements PicocliCommandProvider {
    @Override
    public Object getPicocliCommand() {
        return RootCommand.class;
    }

}
